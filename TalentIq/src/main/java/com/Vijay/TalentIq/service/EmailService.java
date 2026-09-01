package com.Vijay.TalentIq.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp, int expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setTo(toEmail);
            helper.setSubject("TalentIQ - Your Password Reset OTP");
            helper.setText(buildOtpEmailHtml(otp, expiryMinutes, "reset your TalentIQ account password"), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email. Please try again later.");
        }
    }

    public void sendSignupOtpEmail(String toEmail, String otp, int expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setTo(toEmail);
            helper.setSubject("TalentIQ - Verify Your Email");
            helper.setText(buildOtpEmailHtml(otp, expiryMinutes, "verify your TalentIQ account email"), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email. Please try again later.");
        }
    }

    private String buildOtpEmailHtml(String otp, int expiryMinutes, String purpose) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:480px;margin:auto;"
                + "border:1px solid #eee;border-radius:8px;overflow:hidden\">"
                + "<div style=\"background:#4f46e5;padding:20px;text-align:center\">"
                + "<h2 style=\"color:#fff;margin:0\">TalentIQ</h2></div>"
                + "<div style=\"padding:24px;color:#333\">"
                + "<p>Hello,</p>"
                + "<p>We received a request to " + purpose + ". "
                + "Use the OTP below to continue:</p>"
                + "<div style=\"text-align:center;margin:24px 0\">"
                + "<span style=\"display:inline-block;padding:12px 28px;font-size:28px;"
                + "letter-spacing:6px;background:#f3f4f6;border-radius:6px;font-weight:bold\">"
                + otp + "</span></div>"
                + "<p>This OTP is valid for <strong>" + expiryMinutes + " minutes</strong> "
                + "and can be used only once.</p>"
                + "<p>If you did not request this, you can safely ignore this email.</p>"
                + "<p style=\"margin-top:32px;color:#888;font-size:12px\">Regards,<br/>TalentIQ Team</p>"
                + "</div></div>";
    }
}