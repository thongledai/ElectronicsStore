package com.example.store.service.wallet;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.config.PaymentProperties;
import com.example.store.dto.payment.SepayWebhookPayload;
import com.example.store.dto.wallet.WalletSummaryDTO;
import com.example.store.dto.wallet.WalletTopUpDTO;
import com.example.store.dto.wallet.WalletTransactionDTO;
import com.example.store.entity.Order;
import com.example.store.entity.Transaction;
import com.example.store.entity.User;
import com.example.store.entity.WalletTopUpRequest;
import com.example.store.exception.BusinessException;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.TransactionRepository;
import com.example.store.repository.UserRepository;
import com.example.store.repository.WalletTopUpRequestRepository;
import com.example.store.service.payment.VietQrUrlBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements IWalletService {
    private static final String REF_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REF_RANDOM_LENGTH = 8;
    private static final int REF_MAX_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final WalletTopUpRequestRepository topUpRepository;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentProperties paymentProperties;
    private final VietQrUrlBuilder qrUrlBuilder;

    @Override
    @Transactional(readOnly = true)
    public WalletSummaryDTO getSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản ví"));
        assertCustomer(user);
        List<WalletTransactionDTO> transactions = transactionRepository
                .findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(t -> new WalletTransactionDTO(
                        t.getId(),
                        t.getType() == null ? (Boolean.TRUE.equals(t.getIsUp()) ? "WALLET_TOP_UP" : "WALLET_TRANSACTION") : t.getType(),
                        Boolean.TRUE.equals(t.getIsUp()),
                        t.getAmount(),
                        t.getReference(),
                        t.getOrderId(),
                        t.getCreatedAt()))
                .toList();
        return new WalletSummaryDTO(zeroIfNull(user.getEWallet()), transactions);
    }

    @Override
    @Transactional
    public WalletTopUpDTO createTopUp(UUID userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản ví"));
        assertCustomer(user);
        WalletTopUpRequest topUp = WalletTopUpRequest.builder()
                .user(user)
                .paymentReference(generateReference())
                .status(WalletTopUpRequest.Status.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(Math.max(1, paymentProperties.getExpireMinutes())))
                .build();
        topUpRepository.saveAndFlush(topUp);
        return toDTO(topUp);
    }

    @Override
    @Transactional
    public WalletTopUpDTO getTopUp(UUID userId, UUID topUpId) {
        WalletTopUpRequest topUp = topUpRepository.findByIdAndUserIdForUpdate(topUpId, userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy yêu cầu nạp ví"));
        if (isOpen(topUp) && topUp.getExpiresAt() != null && !topUp.getExpiresAt().isAfter(LocalDateTime.now())) {
            topUp.setStatus(WalletTopUpRequest.Status.EXPIRED);
            topUpRepository.save(topUp);
        }
        return toDTO(topUp);
    }

    @Override
    @Transactional
    public TopUpTransferResult processSepayTopUp(UUID topUpId, SepayWebhookPayload payload) {
        WalletTopUpRequest topUp = topUpRepository.findByIdForUpdate(topUpId).orElse(null);
        if (topUp == null) {
            return TopUpTransferResult.NOT_PAYABLE;
        }
        if (topUp.getStatus() == WalletTopUpRequest.Status.PAID) {
            return TopUpTransferResult.DUPLICATE_PAYMENT;
        }
        if (topUp.getStatus() == WalletTopUpRequest.Status.EXPIRED) {
            return TopUpTransferResult.LATE_PAYMENT;
        }
        if (topUp.getStatus() != WalletTopUpRequest.Status.PENDING
                && topUp.getStatus() != WalletTopUpRequest.Status.AMOUNT_MISMATCH) {
            return TopUpTransferResult.NOT_PAYABLE;
        }
        if (topUp.getExpiresAt() != null && !topUp.getExpiresAt().isAfter(LocalDateTime.now())) {
            topUp.setStatus(WalletTopUpRequest.Status.EXPIRED);
            topUpRepository.save(topUp);
            return TopUpTransferResult.LATE_PAYMENT;
        }

        BigDecimal received = payload == null ? null : payload.getTransferAmount();
        if (payload == null || payload.getId() == null || received == null || received.signum() <= 0) {
            return TopUpTransferResult.NOT_PAYABLE;
        }
        topUp.setReceivedAmount(received);

        UUID ownerId = topUp.getUser().getId();
        User user = userRepository.findByIdForUpdate(ownerId).orElse(null);
        if (user == null || !isCustomer(user)) {
            return TopUpTransferResult.NOT_PAYABLE;
        }

        // Tiền thực nhận từ webhook là giá trị duy nhất được cộng vào ví.
        user.setEWallet(zeroIfNull(user.getEWallet()).add(received));
        userRepository.save(user);

        topUp.setStatus(WalletTopUpRequest.Status.PAID);
        topUp.setProviderTransactionId(String.valueOf(payload.getId()));
        topUp.setPaidAt(LocalDateTime.now());
        topUpRepository.save(topUp);

        transactionRepository.save(Transaction.builder()
                .user(user)
                .isUp(true)
                .amount(received)
                .type(Transaction.TYPE_WALLET_TOP_UP)
                .reference(topUp.getPaymentReference())
                .build());
        return TopUpTransferResult.PAID;
    }

    @Override
    @Transactional
    public void debitForOrder(UUID userId, Order order) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy tài khoản ví"));
        assertCustomer(user);
        BigDecimal total = order.getTotal();
        if (total == null || total.signum() < 0) {
            throw new BusinessException("Tổng tiền đơn hàng không hợp lệ");
        }
        BigDecimal balance = zeroIfNull(user.getEWallet());
        if (balance.compareTo(total) < 0) {
            throw new BusinessException("Số dư ví không đủ. Vui lòng nạp tiền hoặc chọn phương thức thanh toán khác.");
        }

        user.setEWallet(balance.subtract(total));
        userRepository.save(user);
        transactionRepository.save(Transaction.builder()
                .user(user)
                .isUp(false)
                .amount(total)
                .type(Transaction.TYPE_ORDER_PAYMENT)
                .reference(order.getId().toString())
                .orderId(order.getId())
                .build());
    }

    private String generateReference() {
        String prefix = paymentProperties.getReferencePrefix() == null
                ? "" : paymentProperties.getReferencePrefix().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (prefix.isBlank()) {
            prefix = "ES";
        }
        for (int attempt = 0; attempt < REF_MAX_ATTEMPTS; attempt++) {
            StringBuilder reference = new StringBuilder(prefix);
            for (int i = 0; i < REF_RANDOM_LENGTH; i++) {
                reference.append(REF_ALPHABET.charAt(RANDOM.nextInt(REF_ALPHABET.length())));
            }
            String candidate = reference.toString();
            if (!orderRepository.existsByPaymentReference(candidate)
                    && !topUpRepository.existsByPaymentReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Không tạo được mã tham chiếu nạp ví duy nhất");
    }

    private WalletTopUpDTO toDTO(WalletTopUpRequest topUp) {
        long secondsRemaining = topUp.getExpiresAt() == null ? 0
                : Math.max(0, Duration.between(LocalDateTime.now(), topUp.getExpiresAt()).getSeconds());
        boolean canTransfer = isOpen(topUp) && secondsRemaining > 0;
        return new WalletTopUpDTO(
                topUp.getId(),
                topUp.getRequestedAmount(),
                topUp.getReceivedAmount(),
                topUp.getStatus().name(),
                topUp.getPaymentReference(),
                paymentProperties.getBankName(),
                paymentProperties.getAccountNumber(),
                paymentProperties.getAccountName(),
                canTransfer ? qrUrlBuilder.build(topUp.getPaymentReference(), null) : null,
                topUp.getExpiresAt(),
                secondsRemaining,
                topUp.getPaidAt());
    }

    private boolean isOpen(WalletTopUpRequest topUp) {
        return topUp.getStatus() == WalletTopUpRequest.Status.PENDING
                || topUp.getStatus() == WalletTopUpRequest.Status.AMOUNT_MISMATCH;
    }

    private void assertCustomer(User user) {
        if (!isCustomer(user)) {
            throw new AccessDeniedException("Chỉ tài khoản Customer mới được sử dụng ví điện tử");
        }
    }

    private boolean isCustomer(User user) {
        return user != null && user.getRole() != null && "CUSTOMER".equalsIgnoreCase(user.getRole().getName());
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
