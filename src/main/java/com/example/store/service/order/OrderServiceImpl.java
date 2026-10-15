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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        private final PaymentMethodRepository paymentMethodRepository;
        private final OrderRepository orderRepository;
        private final OrderStatusRepository orderStatusRepository;
        private final UserRepository userRepository;
        private final ProductVariantRepository variantRepository;
        private final CartItemRepository cartItemRepository;
        private final OrderMapper orderMapper;

        @Override
        @Transactional(readOnly = true)
        public CheckoutPreviewDTO preview(
                        UUID userId,
                        CheckoutPreviewRequest req) {

                List<CartItem> items = cartService.getOwnedItems(userId, req.cartItemIds());

                /*
                 * Chưa tính phí GHN ở đây vì cần gọi quy trình preview
                 * GHN hiện có với địa chỉ và thông tin kiện hàng thực tế.
                 * Không tự ý giả định phí vận chuyển bằng 0.
                 */
                BigDecimal shippingFee = BigDecimal.ZERO;
                PriceBreakdown price = pricingCalculator.calculate(items, shippingFee);

                return new CheckoutPreviewDTO(
                                items.stream()
                                                .map(cartItemMapper::toCartItemDTO)
                                                .toList(),
                                price.subtotal(),
                                price.shippingFee(),
                                price.discount(),
                                price.total());
        }

        @Override
        @Transactional
        public OrderDTO placeOrder(UUID userId, CheckoutRequest req) {

                // 1. Lấy các sản phẩm thuộc giỏ hàng của người dùng.
                List<CartItem> items = cartService.getOwnedItems(userId, req.cartItemIds());

                // 2. Kiểm tra phương thức thanh toán.
                PaymentMethod payment = paymentMethodRepository.findById(req.paymentMethodId())
                                .orElseThrow(() -> new AppException(
                                                OrderErrorCode.PAYMENT_METHOD_NOT_SUPPORTED));

                // 3. Khóa biến thể sản phẩm để tránh bán vượt tồn kho.
                Map<UUID, ProductVariant> variants = lockVariants(items);

                for (CartItem item : items) {
                        ProductVariant variant = variants.get(item.getVariant().getId());

                        if (variant == null) {
                                throw new AppException(
                                                CartErrorCode.CART_ITEM_NOT_FOUND);
                        }

                        Product product = variant.getProduct();

                        if (!product.isActive() || !product.isSelling()) {
                                throw new AppException(
                                                CartErrorCode.CART_ITEM_NOT_FOUND);
                        }

                        if (variant.getQuantity() == null
                                        || variant.getQuantity() < item.getCount()) {
                                throw new AppException(
                                                ProductErrorCode.INSUFFICIENT_STOCK);
                        }
                }

                // 4. Lấy người dùng.
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new AppException(
                                                CartErrorCode.CART_ITEM_NOT_FOUND));

                // 5. Kiểm tra địa chỉ riêng của đơn hàng.
                String address = req.address() == null
                                ? ""
                                : req.address().trim();

                if (address.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Vui lòng nhập địa chỉ nhận hàng.");
                }

                /*
                 * Địa chỉ phải được chọn/kiểm tra bằng danh mục GHN.
                 * Không nên tự suy đoán tỉnh và phường chỉ dựa vào chuỗi.
                 *
                 * Tạm tách địa chỉ theo định dạng:
                 * Địa chỉ chi tiết, Phường/Xã, Tỉnh/Thành phố.
                 */
                String[] addressParts = splitAddress(address);

                /*
                 * Chưa có phí GHN đã xác nhận thì không được tạo đơn
                 * với tổng tiền giả. Phần này cần được nối với kết quả
                 * preview GHN thật trước khi bật luồng đặt hàng.
                 */
                throw new UnsupportedOperationException(
                                "Cần tích hợp phí preview GHN trước khi hoàn tất đặt hàng.");
        }

        /**
         * Tách địa chỉ theo định dạng:
         * Địa chỉ chi tiết, Phường/Xã, Tỉnh/Thành phố.
         */
        private String[] splitAddress(String address) {
                String[] parts = address == null
                                ? new String[0]
                                : address.trim().split("\\s*,\\s*", 3);

                return new String[] {
                                parts.length > 0 && !parts[0].isBlank()
                                                ? parts[0]
                                                : null,
                                parts.length > 1 && !parts[1].isBlank()
                                                ? parts[1]
                                                : null,
                                parts.length > 2 && !parts[2].isBlank()
                                                ? parts[2]
                                                : null
                };
        }

        private Map<UUID, ProductVariant> lockVariants(
                        List<CartItem> items) {

                Set<UUID> ids = items.stream()
                                .map(item -> item.getVariant().getId())
                                .collect(Collectors.toSet());

                return variantRepository.findAllByIdForUpdate(ids)
                                .stream()
                                .collect(Collectors.toMap(
                                                ProductVariant::getId,
                                                Function.identity()));
        }

        private String describe(ProductVariant variant) {
                return variant.getStyleValues()
                                .stream()
                                .map(StyleValue::getName)
                                .collect(Collectors.joining(", "));
        }

        private String firstImage(ProductVariant variant) {
                return variant.getImages()
                                .stream()
                                .findFirst()
                                .map(ProductVariantImage::getImageUrl)
                                .orElse(null);
        }
}