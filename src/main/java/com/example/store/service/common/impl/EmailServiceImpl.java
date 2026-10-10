package com.example.store.service.common.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import com.example.store.service.common.IEmailService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailServiceImpl implements IEmailService {

    private final RestClient restClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public EmailServiceImpl(
            @Value("${brevo.api.key:}") String apiKey,
            @Value("${brevo.sender.email}") String senderEmail,
            @Value("${brevo.sender.name}") String senderName) {

        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", "application/json")
                .build();
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp, String subject, String actionDescription) {
        // Chỉ log khi chạy local để test. Nên bỏ dòng này trên production.
        log.info(">> OTP for [{}]: [{}] ({}) <<", toEmail, otp, actionDescription);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("BREVO_API_KEY is not configured. OTP was printed to logs for testing.");
            return;
        }

        try {
            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", senderEmail, "name", senderName),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", buildOtpHtml(otp, actionDescription)
            );

            restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Sent OTP email successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpHtml(String otp, String actionDescription) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h2 style="color: #2563eb; margin: 0;">TechNova Electronics</h2>
                    <p style="color: #64748b; font-size: 14px; margin-top: 4px;">Hệ thống bán lẻ thiết bị công nghệ hàng đầu</p>
                </div>
                <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                <p style="font-size: 16px; color: #333;">Xin chào,</p>
                <p style="font-size: 15px; color: #475569; line-height: 1.5;">
                    Bạn đang thực hiện <strong>%s</strong> trên hệ thống TechNova Electronics.
                    Vui lòng sử dụng mã xác thực (OTP) dưới đây để tiếp tục:
                </p>
                <div style="text-align: center; margin: 28px 0;">
                    <span style="display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #2563eb; background: #eff6ff; padding: 12px 28px; border-radius: 8px; border: 1px dashed #3b82f6;">
                        %s
                    </span>
                </div>
                <p style="font-size: 14px; color: #64748b;">
                    * Mã OTP này có hiệu lực trong <strong>10 phút</strong>. Vì lý do an toàn, vui lòng không chia sẻ mã này cho bất kỳ ai.
                </p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0 16px;">
                <p style="font-size: 12px; color: #94a3b8; text-align: center;">
                    Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email. Trân trọng cảm ơn!
                </p>
            </div>
            """.formatted(HtmlUtils.htmlEscape(actionDescription), HtmlUtils.htmlEscape(otp));
    }
}