package com.example.store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.example.store.service.IEmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailServiceImpl implements IEmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@technova.com}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otp, String subject, String actionDescription) {
        log.info("=================================================");
        log.info(">> OTP for [{}]: [{}] ({}) <<", toEmail, otp, actionDescription);
        log.info("=================================================");

        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. OTP was printed to logs for testing.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "TechNova Electronics");
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String html = """
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
            """.formatted(actionDescription, otp);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent OTP email successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}. Check SMTP configuration.", toEmail, e.getMessage());
        }
    }
}
