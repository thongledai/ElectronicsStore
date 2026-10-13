package com.example.store.service.delivery.impl;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.store.dto.ghn.GhnAddressOptionDTO;
import com.example.store.dto.ghn.GhnCreateOrderData;
import com.example.store.dto.ghn.GhnCreateOrderRequest;
import com.example.store.dto.ghn.GhnShipmentCreateRequestDTO;
import com.example.store.dto.ghn.GhnShipmentResponseDTO;
import com.example.store.entity.Order;
import com.example.store.entity.OrderStatus;
import com.example.store.enums.OrderStatusName;
import com.example.store.exception.BusinessException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.GhnOrderMapper;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderStatusRepository;
import com.example.store.service.delivery.GhnApiClient;
import com.example.store.service.delivery.IGhnAddressService;
import com.example.store.service.delivery.IGhnDeliveryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GhnDeliveryServiceImpl implements IGhnDeliveryService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final GhnOrderMapper orderMapper;
    private final GhnApiClient apiClient;
    private final IGhnAddressService addressService;

    @Override
    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        if (StringUtils.hasText(order.getGhnOrderCode())) throw new BusinessException("Đơn đã có vận đơn, không thể xác nhận lại.");
        if (currentStatus(order) != OrderStatusName.NOT_PROCESSED) throw new BusinessException("Chỉ xác nhận đơn đang ở trạng thái NOT_PROCESSED.");
        order.setStatus(resolveStatus(OrderStatusName.CONFIRMED));
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void saveAddressSelection(UUID orderId, GhnShipmentCreateRequestDTO input) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        ensureConfirmed(order);
        if (StringUtils.hasText(order.getGhnOrderCode())) throw new BusinessException("Đơn đã có vận đơn GHN; không thể đổi địa chỉ.");
        validateAndApplyAddress(order, input);
    }

    @Override
    @Transactional
    public GhnShipmentResponseDTO createShipment(UUID orderId, GhnShipmentCreateRequestDTO input) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        if (StringUtils.hasText(order.getGhnOrderCode())) return response(order, true);
        ensureConfirmed(order);
        validateAndApplyAddress(order, input);
        validateReady(order);

        GhnCreateOrderRequest request = applyOverrides(orderMapper.toCreateOrderRequest(order), order, input);
        GhnCreateOrderData result = apiClient.createOrder(request);
        order.setGhnOrderCode(result.getOrderCode());
        order.setGhnStatus("created");
        order.setGhnShippingFee(result.getTotalFee());
        if (result.getExpectedDeliveryTime() != null) {
            order.setExpectedDeliveryTime(LocalDateTime.ofInstant(result.getExpectedDeliveryTime(), VIETNAM));
        }
        orderRepository.save(order);
        return response(order, false);
    }

    @Override
    @Transactional(readOnly = true)
    public GhnShipmentResponseDTO getShipment(UUID orderId) {
        Order order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        return response(order, StringUtils.hasText(order.getGhnOrderCode()));
    }

    private void validateAndApplyAddress(Order order, GhnShipmentCreateRequestDTO input) {
        if (!orderMapper.isNewAddressModel()) throw new BusinessException("GHN_NEW_ADDRESS_MODEL phải là true để dùng danh mục địa chỉ 2 cấp.");
        if (input == null || input.getProvinceId() == null || input.getWardId() == null) throw new BusinessException("Hãy chọn tỉnh/thành và phường/xã từ danh mục GHN.");
        String detail = input.getAddressDetail() == null ? "" : input.getAddressDetail().trim();
        if (!StringUtils.hasText(detail)) throw new BusinessException("Nhập số nhà và tên đường trong địa chỉ chi tiết.");
        if (detail.length() > 500) throw new BusinessException("Địa chỉ chi tiết tối đa 500 ký tự.");

        GhnAddressOptionDTO province = addressService.getProvinces().stream()
                .filter(item -> item.getId().equals(input.getProvinceId())).findFirst()
                .orElseThrow(() -> new BusinessException("Tỉnh/thành không còn trong danh mục GHN; hãy tải lại trang."));
        GhnAddressOptionDTO ward = addressService.getWards(province.getId()).stream()
                .filter(item -> item.getId().equals(input.getWardId())).findFirst()
                .orElseThrow(() -> new BusinessException("Phường/xã không thuộc tỉnh/thành đã chọn; hãy chọn lại."));

        order.setToProvinceName(province.getName());
        order.setToWardName(ward.getName());
        order.setToDistrictName(null);
        order.setAddress(detail + ", " + ward.getName() + ", " + provinceAddressLabel(province));
        orderRepository.save(order);
    }

    private String provinceAddressLabel(GhnAddressOptionDTO province) {
        boolean city = province.getExtensionNames() != null && province.getExtensionNames().stream()
                .filter(StringUtils::hasText).map(this::normalize)
                .anyMatch(alias -> alias.startsWith("tp ") || alias.startsWith("thanh pho "));
        return city && !province.getName().regionMatches(true, 0, "TP.", 0, 3)
                ? "TP. " + province.getName() : province.getName();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9]+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private void validateReady(Order order) {
        List<String> invalid = new ArrayList<>();
        if (order.getUser() == null || !StringUtils.hasText(order.getUser().getFullName())) invalid.add("tên người nhận");
        if (GhnOrderMapper.normalizePhone(order.getPhone()) == null) invalid.add("số điện thoại 10 số");
        if (!StringUtils.hasText(order.getAddress())) invalid.add("địa chỉ");
        if (!StringUtils.hasText(order.getToWardName())) invalid.add("phường/xã");
        if (!StringUtils.hasText(order.getToProvinceName())) invalid.add("tỉnh/thành");
        if (order.getItems() == null || order.getItems().isEmpty()) invalid.add("sản phẩm trong đơn");
        if (!invalid.isEmpty()) throw new BusinessException("Thiếu dữ liệu tạo vận đơn: " + String.join(", ", invalid) + ".");
        if (orderMapper.calculateTotalWeightGram(order) <= 0 || orderMapper.calculateTotalWeightGram(order) > GhnOrderMapper.MAX_WEIGHT_GRAM_SERVICE_2) {
            throw new BusinessException("Tổng khối lượng phải từ 1 đến 19.999 gram cho dịch vụ GHN đang cấu hình.");
        }
        if (orderMapper.calculateCodAmount(order) > GhnOrderMapper.MAX_COD_AMOUNT) throw new BusinessException("Tiền thu hộ vượt giới hạn GHN.");
    }

    private GhnCreateOrderRequest applyOverrides(GhnCreateOrderRequest base, Order order, GhnShipmentCreateRequestDTO input) {
        int weight = input.getWeight() == null ? base.getWeight() : input.getWeight();
        int length = input.getLength() == null ? base.getLength() : input.getLength();
        int width = input.getWidth() == null ? base.getWidth() : input.getWidth();
        int height = input.getHeight() == null ? base.getHeight() : input.getHeight();
        int payment = input.getPaymentTypeId() == null ? base.getPaymentTypeId() : input.getPaymentTypeId();
        String note = StringUtils.hasText(input.getRequiredNote()) ? input.getRequiredNote().trim().toUpperCase(Locale.ROOT) : base.getRequiredNote();
        if (weight <= 0 || weight > 19_999 || length <= 0 || width <= 0 || height <= 0 || length > 200 || width > 200 || height > 200) {
            throw new BusinessException("Thông số kiện hàng không hợp lệ theo dịch vụ GHN.");
        }
        if (payment != 1 && payment != 2) throw new BusinessException("payment_type_id phải là 1 hoặc 2.");
        if (!List.of("CHOTHUHANG", "CHOXEMHANGKHONGTHU", "KHONGCHOXEMHANG").contains(note)) throw new BusinessException("required_note không được GHN hỗ trợ.");
        return GhnCreateOrderRequest.builder().paymentTypeId(payment).requiredNote(note)
                .clientOrderCode(base.getClientOrderCode()).toName(base.getToName()).toPhone(base.getToPhone())
                .toAddress(base.getToAddress()).toWardName(base.getToWardName()).toDistrictName(null)
                .toProvinceName(base.getToProvinceName()).isNewToAddress(true).serviceTypeId(base.getServiceTypeId())
                .weight(weight).length(length).width(width).height(height).codAmount(base.getCodAmount())
                .orderValue(base.getOrderValue()).insuranceValue(base.getInsuranceValue()).items(base.getItems()).build();
    }

    private void ensureConfirmed(Order order) {
        if (currentStatus(order) != OrderStatusName.CONFIRMED) throw new BusinessException("Chỉ thao tác vận đơn khi đơn đã được xác nhận.");
    }

    private OrderStatusName currentStatus(Order order) {
        return OrderStatusName.fromDbName(order.getStatus().getName()).orElse(null);
    }

    private OrderStatus resolveStatus(OrderStatusName target) {
        return orderStatusRepository.findAll().stream()
                .filter(status -> OrderStatusName.fromDbName(status.getName()).orElse(null) == target)
                .findFirst().orElseThrow(() -> new BusinessException("Thiếu trạng thái đơn hàng " + target + " trong order_status."));
    }

    private GhnShipmentResponseDTO response(Order order, boolean alreadyCreated) {
        return GhnShipmentResponseDTO.builder().orderId(order.getId())
                .orderStatus(order.getStatus().getName()).ghnOrderCode(order.getGhnOrderCode())
                .ghnStatus(order.getGhnStatus()).ghnShippingFee(order.getGhnShippingFee())
                .expectedDeliveryTime(order.getExpectedDeliveryTime()).alreadyCreated(alreadyCreated).build();
    }
}
