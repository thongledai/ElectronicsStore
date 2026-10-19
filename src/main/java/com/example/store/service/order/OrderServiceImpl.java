package com.example.store.service.order;

import com.example.store.dto.ghn.GhnCreateOrderResponse;
import com.example.store.dto.order.*;
import com.example.store.entity.*;
import com.example.store.enums.error.CartErrorCode;
import com.example.store.enums.error.OrderErrorCode;
import com.example.store.enums.error.ProductErrorCode;
import com.example.store.exception.AppException;
import com.example.store.mapper.CartItemMapper;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.*;
import com.example.store.service.cart.ICartService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

        PriceBreakdown price = pricingCalculator.calculate(items, shippingFee);

        String fullAddress = String.join(", ", req.detailAddress(), req.wardName(), req.provinceName());

        return new OrderPreviewDTO(
                null, null, null,
                req.toName(), req.phone(), fullAddress,
                null,                                   // items: trang không dùng
                price.subtotal(), price.shippingFee(), price.discount(), price.total(),
                "COD", false, req.note(),
                ghn.getExpectedDeliveryTime());
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

        // 5. Tính tiền sản phẩm và discount
        PriceBreakdown price = pricingCalculator.calculate(items, shipmentFee);

        // 6. Tạo Order
        OrderStatus initial = orderStatusRepository.findByNameIgnoreCase("NOT_PROCESSED")
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_STATUS_NOT_CONFIGURED));

        String fullAddress = String.join(", ", req.detailAddress(), req.wardName(), req.provinceName());
        LocalDateTime expectedDiliveryDate = ZonedDateTime.parse(ghnCreateOrderResponse.getExpectedDeliveryTime())
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
                .expectedDeliveryDate(expectedDiliveryDate)
                .build();

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

        // 8. Lưu (cascade lưu luôn OrderItem)
        orderRepository.save(order);

        // 9. Xóa các dòng đã mua khỏi giỏ
        cartItemRepository.deleteAll(items);

        return orderMapper.toOrderReviewDTO(order);
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
