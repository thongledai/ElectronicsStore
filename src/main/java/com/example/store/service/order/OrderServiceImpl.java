package com.example.store.service.order;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final ICartService cartService;
    private final PricingCalculator pricingCalculator;
    private final CartItemMapper cartItemMapper;
    private final DeliveryRepository deliveryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public CheckoutPreviewDTO preview(UUID userId, CheckoutPreviewRequest req) {
        List<CartItem> items = cartService.getOwnedItems(userId, req.cartItemIds());
        Delivery delivery = resolveDelivery(req.deliveryId());
        PriceBreakdown price = pricingCalculator.calculate(items, delivery);

        return new CheckoutPreviewDTO(
                items.stream().map(cartItemMapper::toCartItemDTO).toList(),
                delivery.getId(),
                price.subtotal(), price.shippingFee(), price.discount(), price.total());
    }

    private Delivery resolveDelivery(UUID deliveryId) {
        if (deliveryId != null) {
            return deliveryRepository.findById(deliveryId)
                    .filter(Delivery::getIsActive)
                    .orElseThrow(() -> new AppException(OrderErrorCode.DELIVERY_NOT_AVAILABLE));
        }

        // Tu dong chon phuong thuc dang duoc hoat dong
        return deliveryRepository.findByIsActiveTrue().stream()
                .findFirst()
                .orElseThrow(() -> new AppException(OrderErrorCode.DELIVERY_NOT_AVAILABLE));
    }

    @Override
    @Transactional
    public OrderDTO placeOrder(UUID userId, CheckoutRequest req) {

        // 1-3. Lấy dữ liệu đầu vào và kiểm tra
        List<CartItem> items = cartService.getOwnedItems(userId, req.cartItemIds());
        Delivery delivery = resolveDelivery(req.deliveryId());
        PaymentMethod payment = paymentMethodRepository.findById(req.paymentMethodId())
                .orElseThrow(() -> new AppException(OrderErrorCode.PAYMENT_METHOD_NOT_SUPPORTED));

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

        // 5. Tính tiền từ giá trong DB
        PriceBreakdown price = pricingCalculator.calculate(items, delivery);

        // 6. Tạo Order
        OrderStatus initial = orderStatusRepository.findByName(OrderStatusName.NOT_PROCESSED)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_STATUS_NOT_CONFIGURED));

        Order order = Order.builder()
                .user(userRepository.getReferenceById(userId))
                .delivery(delivery)
                .payment(payment)
                .status(initial)
                .phone(req.phone())
                .address(req.address())
                .note(req.note())
                .subtotal(price.subtotal())
                .shippingFee(price.shippingFee())
                .discount(price.discount())
                .total(price.total())
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

        return orderMapper.toOrderDTO(order);
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
