package com.example.store.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.store.config.PaymentProperties;
import com.example.store.dto.payment.SepayWebhookPayload;
import com.example.store.dto.payment.VietQrPaymentDTO;
import com.example.store.entity.BankTransferEvent;
import com.example.store.entity.Order;
import com.example.store.entity.OrderItem;
import com.example.store.entity.OrderStatus;
import com.example.store.entity.ProductVariant;
import com.example.store.enums.PaymentStatus;
import com.example.store.enums.error.OrderErrorCode;
import com.example.store.exception.AppException;
import com.example.store.repository.BankTransferEventRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderStatusRepository;
import com.example.store.repository.ProductVariantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VietQrPaymentServiceImpl implements IVietQrPaymentService {
    private static final String PROVIDER = "SEPAY";
    private static final String QR_BASE = "https://img.vietqr.io/image/";
    /** Bỏ các ký tự dễ nhầm (0/O, 1/I) để khách gõ tay không sai. */
    private static final String REF_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REF_RANDOM_LENGTH = 8;
    private static final int REF_MAX_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentProperties props;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final ProductVariantRepository variantRepository;
    private final BankTransferEventRepository eventRepository;

    // ------------------------------------------------------------------ tạo thông tin thanh toán

    @Override
    public void initPayment(Order order) {
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPaymentReference(generateReference());
        order.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(Math.max(1, props.getExpireMinutes())));
        order.setIsPaidBefore(false);
    }

    private String generateReference() {
        for (int attempt = 0; attempt < REF_MAX_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder(referencePrefix());
            for (int i = 0; i < REF_RANDOM_LENGTH; i++) {
                sb.append(REF_ALPHABET.charAt(RANDOM.nextInt(REF_ALPHABET.length())));
            }
            String ref = sb.toString();
            if (!orderRepository.existsByPaymentReference(ref)) {
                return ref;
            }
        }
        throw new IllegalStateException("Không tạo được mã tham chiếu thanh toán duy nhất");
    }

    /** Chuẩn hóa tiền tố: chỉ chữ/số in hoa, để khớp cả khi tạo mã lẫn khi tách mã khỏi nội dung chuyển khoản. */
    private String referencePrefix() {
        String p = props.getReferencePrefix() == null ? "" : props.getReferencePrefix();
        p = p.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return p.isEmpty() ? "ES" : p;
    }

    // ------------------------------------------------------------------ xem QR / trạng thái

    @Override
    @Transactional(readOnly = true)
    public VietQrPaymentDTO getPayment(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        if (!order.usesBankTransfer() || order.getPaymentReference() == null) {
            throw new AppException(OrderErrorCode.BANK_TRANSFER_NOT_APPLICABLE);
        }

        long secondsRemaining = 0;
        if (order.getPaymentExpiresAt() != null) {
            secondsRemaining = Math.max(0, Duration.between(LocalDateTime.now(), order.getPaymentExpiresAt()).getSeconds());
        }
        boolean payable = order.getPaymentStatus() == PaymentStatus.UNPAID && secondsRemaining > 0;

        return new VietQrPaymentDTO(
                order.getId(),
                "#ORD-" + order.getId().toString().substring(0, 8).toUpperCase(),
                order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null,
                order.getTotal(),
                order.getPaymentReference(),
                props.getBankName(),
                props.getAccountNumber(),
                props.getAccountName(),
                payable ? buildQrImageUrl(order.getTotal(), order.getPaymentReference()) : null,
                order.getPaymentExpiresAt(),
                secondsRemaining,
                order.getPaidAt());
    }

    /** VietQR Quick Link: https://vietqr.io/danh-sach-api/link-tao-ma-nhanh */
    String buildQrImageUrl(BigDecimal amount, String reference) {
        String path = QR_BASE + props.getBankBin() + "-" + props.getAccountNumber() + "-" + props.getQrTemplate() + ".png";
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(path)
                .queryParam("amount", amount.setScale(0, RoundingMode.HALF_UP).longValue())
                .queryParam("addInfo", reference);
        if (StringUtils.hasText(props.getAccountName())) {
            b.queryParam("accountName", props.getAccountName());
        }
        return b.build().encode().toUriString();
    }

    // ------------------------------------------------------------------ webhook tiền vào

    @Override
    @Transactional
    public TransferResult handleSepayTransfer(SepayWebhookPayload p) {
        if (p == null || p.getId() == null) {
            throw new IllegalArgumentException("Webhook thiếu mã giao dịch");
        }
        String txId = String.valueOf(p.getId());

        if (eventRepository.existsByProviderAndProviderTransactionId(PROVIDER, txId)) {
            return TransferResult.DUPLICATE_EVENT;
        }

        BankTransferEvent event = BankTransferEvent.builder()
                .provider(PROVIDER)
                .providerTransactionId(txId)
                .bankReferenceCode(truncate(p.getReferenceCode(), 100))
                .accountNumber(truncate(p.getAccountNumber(), 30))
                .amount(p.getTransferAmount())
                .transferType(truncate(p.getTransferType(), 10))
                .content(truncate(p.getContent(), 500))
                .build();

        // 1. Chỉ xử lý tiền vào
        if (!"in".equalsIgnoreCase(p.getTransferType())) {
            log.warn("Webhook SePay tx={} bị từ chối: giao dịch không phải tiền vào", txId);
            return finish(event, TransferResult.NOT_INCOMING);
        }

        // 2. Phải đúng tài khoản nhận tiền đã cấu hình
        String configuredAccount = normalizeAccount(props.getAccountNumber());
        if (configuredAccount.isEmpty()
                || (!configuredAccount.equals(normalizeAccount(p.getAccountNumber()))
                    && !configuredAccount.equals(normalizeAccount(p.getSubAccount())))) {
            log.warn("Webhook SePay tx={} sai tài khoản nhận (đã kiểm tra accountNumber và subAccount)", txId);
            return finish(event, TransferResult.ACCOUNT_MISMATCH);
        }

        // 3. Tìm đơn theo mã tham chiếu trong nội dung chuyển khoản (khóa dòng để tuần tự hóa các webhook trùng)
        Order order = null;
        for (String candidate : extractReferenceCandidates(p)) {
            order = orderRepository.findByPaymentReferenceForUpdate(candidate).orElse(null);
            if (order != null) break;
        }
        if (order == null) {
            log.warn("Webhook SePay tx={} không tìm thấy đơn cho mã thanh toán trong code/content", txId);
            return finish(event, TransferResult.ORDER_NOT_FOUND);
        }
        event.setOrderId(order.getId());
        event.setPaymentReference(order.getPaymentReference());

        if (!order.usesBankTransfer()) {
            return finish(event, TransferResult.NOT_PAYABLE);
        }

        // 4. Máy trạng thái thanh toán
        PaymentStatus current = order.getPaymentStatus();
        if (current == PaymentStatus.PAID) {
            log.warn("Đơn {} đã PAID nhưng nhận thêm giao dịch tx={}: cần hoàn tiền thủ công", order.getId(), txId);
            return finish(event, TransferResult.DUPLICATE_PAYMENT);
        }
        if (current == PaymentStatus.EXPIRED || current == PaymentStatus.CANCELLED) {
            // Tiền đến sau khi đơn đã hết hạn/hủy: không tự khôi phục đơn, đánh dấu để nhân viên đối soát
            order.setPaymentStatus(PaymentStatus.NEEDS_RECONCILIATION);
            orderRepository.save(order);
            log.warn("Đơn {} nhận tiền muộn tx={}: cần đối soát", order.getId(), txId);
            return finish(event, TransferResult.LATE_PAYMENT);
        }
        if (current != PaymentStatus.UNPAID) {
            return finish(event, TransferResult.NOT_PAYABLE);
        }
        if (!"NOT_PROCESSED".equalsIgnoreCase(order.getStatus().getName())) {
            order.setPaymentStatus(PaymentStatus.NEEDS_RECONCILIATION);
            orderRepository.save(order);
            return finish(event, TransferResult.LATE_PAYMENT);
        }

        // 5. Số tiền phải khớp chính xác với tổng tiền backend đã chốt
        BigDecimal paid = p.getTransferAmount();
        if (paid == null || paid.compareTo(order.getTotal()) != 0) {
            order.setPaymentStatus(PaymentStatus.NEEDS_RECONCILIATION);
            orderRepository.save(order);
            log.warn("Đơn {} sai số tiền tx={} (nhận {}, cần {})", order.getId(), txId, paid, order.getTotal());
            return finish(event, TransferResult.AMOUNT_MISMATCH);
        }

        // 6. Hợp lệ: đánh dấu đã thanh toán (isPaidBefore = true để GHN gửi cod_amount = 0)
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setIsPaidBefore(true);
        order.setPaidAt(LocalDateTime.now());
        if (props.isAutoConfirmAfterPaid()) {
            OrderStatus confirmed = orderStatusRepository.findByNameIgnoreCase("CONFIRMED")
                    .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_STATUS_NOT_CONFIGURED));
            order.setStatus(confirmed);
        }
        orderRepository.save(order);
        log.info("Đơn {} đã được xác minh thanh toán qua tx={}", order.getId(), txId);
        return finish(event, TransferResult.PAID);
    }

    /** Lưu nhật ký (flush ngay để unique constraint chặn webhook trùng chạy song song). */
    private TransferResult finish(BankTransferEvent event, TransferResult result) {
        event.setResult(result.name());
        eventRepository.saveAndFlush(event);
        return result;
    }

    private Set<String> extractReferenceCandidates(SepayWebhookPayload p) {
        String haystack = ((p.getCode() == null ? "" : p.getCode()) + " "
                + (p.getContent() == null ? "" : p.getContent())).toUpperCase();
        // Lookahead để lấy cả các ứng viên chồng lấn khi nội dung bị dính liền không có dấu phân cách
        Matcher m = Pattern.compile("(?=(" + Pattern.quote(referencePrefix()) + "[A-Z0-9]{" + REF_RANDOM_LENGTH + "}))")
                .matcher(haystack);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    // ------------------------------------------------------------------ hết hạn thanh toán

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findExpiredOrderIds() {
        return orderRepository.findExpiredPaymentOrderIds(PaymentStatus.UNPAID, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void expireOrder(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || !order.usesBankTransfer() || order.getPaymentStatus() != PaymentStatus.UNPAID) {
            return; // đã thanh toán / đã xử lý ở luồng khác
        }
        if (order.getPaymentExpiresAt() == null || order.getPaymentExpiresAt().isAfter(LocalDateTime.now())) {
            return;
        }
        if (!"NOT_PROCESSED".equalsIgnoreCase(order.getStatus().getName())) {
            return;
        }

        OrderStatus cancelled = orderStatusRepository.findByNameIgnoreCase("CANCELLED")
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_STATUS_NOT_CONFIGURED));

        restoreStock(order);
        order.setStatus(cancelled);
        order.setPaymentStatus(PaymentStatus.EXPIRED);
        orderRepository.save(order);
        log.info("Đơn {} quá hạn thanh toán: đã hủy và hoàn tồn kho", orderId);
    }

    private void restoreStock(Order order) {
        Set<UUID> variantIds = order.getItems().stream()
                .filter(i -> i.getVariant() != null)
                .map(i -> i.getVariant().getId())
                .collect(Collectors.toSet());
        if (variantIds.isEmpty()) return;

        Map<UUID, ProductVariant> variants = variantRepository.findAllByIdForUpdate(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() == null || item.getQuantity() == null || item.getQuantity() <= 0) continue;
            ProductVariant v = variants.get(item.getVariant().getId());
            if (v != null) {
                v.setQuantity((v.getQuantity() == null ? 0 : v.getQuantity()) + item.getQuantity());
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static String normalizeAccount(String s) {
        return s == null ? "" : s.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
