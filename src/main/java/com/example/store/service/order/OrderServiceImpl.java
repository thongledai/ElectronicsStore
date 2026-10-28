package com.example.store.service.order;

import com.example.store.dto.ghn.GhnCreateOrderResponse;
import com.example.store.dto.order.*;
import com.example.store.entity.*;
import com.example.store.enums.PaymentStatus;
import com.example.store.enums.error.CartErrorCode;
import com.example.store.enums.error.OrderErrorCode;
import com.example.store.enums.error.ProductErrorCode;
import com.example.store.enums.error.PromotionErrorCode;
import com.example.store.exception.AppException;
import com.example.store.mapper.CartItemMapper;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.*;
import com.example.store.service.cart.ICartService;
import com.example.store.service.payment.IVietQrPaymentService;
import com.example.store.service.wallet.IWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ICartService cartService;
    private final PricingCalculator pricingCalculator;
    private final CartItemMapper cartItemMapper;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;
    private final IShipmentService shipmentService;
    private final IVietQrPaymentService vietQrPaymentService;
    private final IWalletService walletService;
    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public CheckoutPreviewDTO checkout(UUID userId, CheckoutRequest req) {
        List<CartItem> items = cartService.getOwnedItems(userId, req.cartItemIds());
        PriceBreakdown price = pricingCalculator.calculate(items, null);

        return new CheckoutPreviewDTO(
                items.stream().map(cartItemMapper::toCartItemDTO).toList(),
                price.subtotal(), price.discount(), price.total());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPreviewDTO createOrderPreview(UUID userId, OrderPreviewRequest req) {
        List<CartItem> items = cartService.getOwnedItems(userId, req.cartItemIds());

        GhnCreateOrderResponse ghn = shipmentService.createOrderPreview(req);
        BigDecimal shippingFee = ghn.getTotalFee();

        // Kiểm tra voucher xem có hợp lệ để áp dụng tính toán trước không
        Promotion promotion = null;
        if (StringUtils.hasText(req.voucherCode())) {
            promotion = promotionRepository.findByCodeIgnoreCase(req.voucherCode().trim()).orElse(null);
            if (promotion != null) {
                // Kiểm tra sơ bộ các điều kiện
                try {
                    PriceBreakdown basePrice = pricingCalculator.calculate(items, BigDecimal.ZERO, null);
                    validatePromotionState(promotion, basePrice.subtotal());
                } catch (Exception e) {
                    promotion = null; // Nếu không đạt điều kiện thì không áp dụng
                }
            }
        }

        PriceBreakdown price = pricingCalculator.calculate(items, shippingFee, promotion);

        String fullAddress = String.join(", ", req.detailAddress(), req.wardName(), req.provinceName());

        PaymentMethod payment = paymentMethodRepository.findById(req.paymentMethodId())
                .orElseThrow(() -> new AppException(OrderErrorCode.PAYMENT_METHOD_NOT_SUPPORTED));

        return new OrderPreviewDTO(
                null, null, null,
                req.toName(), req.phone(), fullAddress,
                null,
                price.subtotal(), price.shippingFee(), price.discount(), price.total(),
                payment.getName(), false, req.note(),
                ghn.getExpectedDeliveryTime(),
                promotion != null ? promotion.getCode() : null,
                price.voucherDiscount());
    }

    @Override
    @Transactional
    public OrderPreviewDTO placeOrder(UUID userId, OrderPreviewRequest req) {

        // 1-3. Lấy dữ liệu đầu vào và kiểm tra
        List<CartItem> items = cartService.getOwnedItems(userId, req.cartItemIds());
        PaymentMethod payment = paymentMethodRepository.findById(req.paymentMethodId())
                .orElseThrow(() -> new AppException(OrderErrorCode.PAYMENT_METHOD_NOT_SUPPORTED));

        // Tính tiền vận chuyển
        GhnCreateOrderResponse ghnCreateOrderResponse = shipmentService.createOrderPreview(req);
        BigDecimal shipmentFee = ghnCreateOrderResponse.getTotalFee();

        // 4. Khóa variant, kiểm tra bán được và đủ kho
        Map<UUID, ProductVariant> variants = lockVariants(items);
        for (CartItem ci : items) {
            ProductVariant v = variants.get(ci.getVariant().getId());
            Product p = v.getProduct();
            if (!p.isActive() || !p.isSelling()) {
                throw new AppException(CartErrorCode.CART_ITEM_NOT_FOUND);
            }
            if (v.getQuantity() == null || v.getQuantity() < ci.getCount()) {
                throw new AppException(ProductErrorCode.INSUFFICIENT_STOCK);
            }
        }

        // 5. Kiểm tra và áp dụng Voucher (nếu có)
        Promotion promotion = null;
        PriceBreakdown basePrice = pricingCalculator.calculate(items, BigDecimal.ZERO, null);
        if (StringUtils.hasText(req.voucherCode())) {
            promotion = promotionRepository.findByCodeForUpdate(req.voucherCode().trim())
                    .orElseThrow(() -> new AppException(PromotionErrorCode.VOUCHER_NOT_FOUND));

            validatePromotionState(promotion, basePrice.subtotal());

            // Trừ 1 lượt sử dụng voucher
            promotion.setQuantity(promotion.getQuantity() - 1);
            promotionRepository.save(promotion);
        }

        PriceBreakdown price = pricingCalculator.calculate(items, shipmentFee, promotion);

        // 6. Tạo Order
        OrderStatus initial = orderStatusRepository.findByNameIgnoreCase("NOT_PROCESSED")
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_STATUS_NOT_CONFIGURED));

        String fullAddress = String.join(", ", req.detailAddress(), req.wardName(), req.provinceName());
        LocalDateTime expectedDeliveryDate = ZonedDateTime.parse(ghnCreateOrderResponse.getExpectedDeliveryTime())
                .withZoneSameInstant(VIETNAM).toLocalDateTime();

        Order order = Order.builder()
                .user(userRepository.getReferenceById(userId))
                .payment(payment)
                .status(initial)
                .toName(req.toName())
                .phone(req.phone())
                .detailAddress(req.detailAddress())
                .wardName(req.wardName())
                .provinceName(req.provinceName())
                .fullAddress(fullAddress)
                .note(req.note())
                .subtotal(price.subtotal())
                .shippingFee(price.shippingFee())
                .discount(price.discount())
                .total(price.total())
                .expectedDeliveryDate(expectedDeliveryDate)
                .build();

        // Nếu có áp dụng voucher, gắn vào quan hệ order_promotions
        if (promotion != null) {
            order.getPromotions().add(promotion);
        }

        // Đơn chuyển khoản VietQR: sinh mã tham chiếu, trạng thái UNPAID và hạn thanh toán
        if (order.usesBankTransfer()) {
            vietQrPaymentService.initPayment(order);
        } else if (order.usesEWallet()) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setIsPaidBefore(true);
            order.setPaidAt(LocalDateTime.now());
        }

        for (CartItem ci : items) {
            ProductVariant v = variants.get(ci.getVariant().getId());
            order.addItem(OrderItem.builder()
                    .variant(v)
                    .productName(v.getProduct().getName())
                    .variantDescription(describe(v))
                    .imageUrl(firstImage(v))
                    .unitPrice(v.getPrice())
                    .quantity(ci.getCount())
                    .subtotal(v.getPrice().multiply(BigDecimal.valueOf(ci.getCount())))
                    .build());

            // 7. Trừ kho
            v.setQuantity(v.getQuantity() - ci.getCount());
        }

        // 8. Lưu (cascade lưu luôn OrderItem và quan hệ promotions)
        orderRepository.save(order);

        // Trừ ví và ghi lịch sử trong cùng transaction với việc tạo Order.
        // Nếu số dư không đủ, exception sẽ rollback cả Order, tồn kho và giỏ hàng.
        if (order.usesEWallet()) {
            walletService.debitForOrder(userId, order);
        }

        // 9. Xóa các dòng đã mua khỏi giỏ
        cartItemRepository.deleteAll(items);

        return orderMapper.toOrderReviewDTO(order);
    }

    private void validatePromotionState(Promotion promotion, BigDecimal subtotal) {
        if (!Boolean.TRUE.equals(promotion.getIsActive()) || !Boolean.TRUE.equals(promotion.getIsEnabled())) {
            throw new AppException(PromotionErrorCode.VOUCHER_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            throw new AppException(PromotionErrorCode.VOUCHER_NOT_STARTED);
        }

        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            throw new AppException(PromotionErrorCode.VOUCHER_EXPIRED);
        }

        if (promotion.getQuantity() != null && promotion.getQuantity() <= 0) {
            throw new AppException(PromotionErrorCode.VOUCHER_OUT_OF_STOCK);
        }

        BigDecimal minOrder = promotion.getMinOrderValue() != null ? promotion.getMinOrderValue() : BigDecimal.ZERO;
        if (subtotal != null && subtotal.compareTo(minOrder) < 0) {
            throw new AppException(PromotionErrorCode.VOUCHER_MIN_ORDER_NOT_MET);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderResponseDTO> getCustomerOrders(UUID userId, String status) {
        String filter = normalizeStatus(status);
        List<Order> orders;

        if (filter == null) {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else if ("SHIPPING".equalsIgnoreCase(filter)) {
            orders = orderRepository.findByUserIdAndStatus_NameInIgnoreCaseOrderByCreatedAtDesc(
                    userId, List.of("SHIPPING", "CONFIRMED"));
        } else {
            orders = orderRepository.findByUserIdAndStatus_NameIgnoreCaseOrderByCreatedAtDesc(userId, filter);
        }

        return orders.stream()
                .map(orderMapper::toCustomerOrderResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOrderResponseDTO getCustomerOrderDetail(UUID userId, UUID orderId) {
        Order order = orderRepository.findDetailedByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        return orderMapper.toCustomerOrderResponseDTO(order);
    }

    @Override
    @Transactional
    public void cancelCustomerOrder(UUID userId, UUID orderId, CancelOrderRequest req) {
        Order order = orderRepository.findDetailedByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        String currentStatus = order.getStatus() != null ? order.getStatus().getName() : "";
        if (!"NOT_PROCESSED".equalsIgnoreCase(currentStatus)) {
            throw new AppException(OrderErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        if (order.usesBankTransfer()) {
            PaymentStatus ps = order.getPaymentStatus();
            if (ps == PaymentStatus.PAID) {
                throw new AppException(OrderErrorCode.PAID_ORDER_CANNOT_BE_CANCELLED);
            }
            if (ps == PaymentStatus.NEEDS_RECONCILIATION) {
                throw new AppException(OrderErrorCode.PAYMENT_UNDER_RECONCILIATION);
            }
            if (ps == PaymentStatus.UNPAID) {
                order.setPaymentStatus(PaymentStatus.CANCELLED);
            }
        }

        OrderStatus cancelled = orderStatusRepository.findByNameIgnoreCase("CANCELLED")
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_STATUS_NOT_CONFIGURED));

        order.setStatus(cancelled);

        // Bổ sung lý do hủy vào ghi chú đơn hàng nếu có
        if (req != null && StringUtils.hasText(req.reason())) {
            String note = order.getNote();
            String reasonText = "[Lý do hủy đơn: " + req.reason().trim() + "]";
            order.setNote(StringUtils.hasText(note) ? note + " " + reasonText : reasonText);
        }

        // Hoàn trả lại số lượng tồn kho cho các variant trong đơn
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                ProductVariant variant = item.getVariant();
                int currentQty = variant.getQuantity() != null ? variant.getQuantity() : 0;
                variant.setQuantity(currentQty + item.getQuantity());
                variantRepository.save(variant);
            }
        }

        // Hoàn lại lượt sử dụng voucher nếu đơn hàng có áp dụng voucher
        if (order.getPromotions() != null && !order.getPromotions().isEmpty()) {
            for (Promotion promo : order.getPromotions()) {
                if (promo != null) {
                    promo.setQuantity((promo.getQuantity() != null ? promo.getQuantity() : 0) + 1);
                    promotionRepository.save(promo);
                }
            }
        }

        orderRepository.save(order);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status) || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    private Map<UUID, ProductVariant> lockVariants(List<CartItem> items) {
        Set<UUID> ids = items.stream().map(i -> i.getVariant().getId()).collect(Collectors.toSet());
        return variantRepository.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
    }

    private String describe(ProductVariant v) {
        return v.getStyleValues().stream()
                .map(StyleValue::getName).collect(Collectors.joining(", "));
    }

    private String firstImage(ProductVariant v) {
        return v.getImages().stream().findFirst()
                .map(ProductVariantImage::getImageUrl).orElse(null);
    }
}
