package com.Vijay.TalentIq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOtpEmail(String toEmail, String otp, int expiryMinutes) {
        sendEmail(toEmail, "TalentIQ - Your Password Reset OTP",
            buildOtpEmailHtml(otp, expiryMinutes, "reset your TalentIQ account password"));
    }

    public void sendSignupOtpEmail(String toEmail, String otp, int expiryMinutes) {
        sendEmail(toEmail, "TalentIQ - Verify Your Email",
            buildOtpEmailHtml(otp, expiryMinutes, "verify your TalentIQ account email"));
    }

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("api-key", brevoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("accept", "application/json");

            Map<String, Object> sender = new HashMap<>();
            sender.put("name", senderName);
            sender.put("email", senderEmail);

            Map<String, Object> recipient = new HashMap<>();
            recipient.put("email", toEmail);

            Map<String, Object> body = new HashMap<>();
            body.put("sender", sender);
            body.put("to", new Object[] { recipient });
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(BREVO_API_URL, request, String.class);
            log.info("Brevo email sent successfully to {}", toEmail);

        } catch (HttpClientErrorException e) {
            HttpStatusCode status = e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            log.error("Brevo API rejected the request. Status: {} Body: {}", status, responseBody);
            throw new RuntimeException("Failed to send email via Brevo. Please try again later.");
        } catch (Exception e) {
            log.error("Unexpected error calling Brevo API", e);
            throw new RuntimeException("Failed to send email via Brevo. Please try again later.");
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