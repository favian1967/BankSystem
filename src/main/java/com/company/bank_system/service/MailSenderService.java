package com.company.bank_system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends transactional email via the Brevo HTTP API (https://api.brevo.com).
 *
 * <p>We use an HTTP provider on port 443 instead of SMTP because many ISPs/networks block
 * outbound SMTP (ports 25/465/587), which shows up as "Got bad greeting … [EOF]" /
 * "SSL peer shut down incorrectly". HTTPS always works, so email delivery is reliable anywhere.
 *
 * <p>Setup: create a free Brevo account, verify a sender address, generate an API key, then set
 * {@code BREVO_API_KEY} and {@code MAIL_SENDER_EMAIL} (the verified sender) in the environment.
 */
@Service
@Slf4j
public class MailSenderService {

    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MailSenderService(
            @Value("${app.mail.brevo.api-key:}") String apiKey,
            @Value("${app.mail.brevo.sender-email:}") String senderEmail,
            @Value("${app.mail.brevo.sender-name:Aurora Bank}") String senderName
    ) {
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Sends a plain-text email. */
    public void send(String to, String subject, String text) {
        postToBrevo(to, subject, null, text);
    }

    /** Sends the branded Aurora Bank email-verification message with a big, styled code. */
    public void sendVerificationCode(String to, String code) {
        String html = VERIFICATION_TEMPLATE.replace("{{CODE}}", code);
        String text = "Your Aurora Bank verification code is " + code + ". It expires in 15 minutes. "
                + "If you didn't request this, ignore this email.";
        postToBrevo(to, "Your Aurora Bank verification code", html, text);
    }

    private void postToBrevo(String to, String subject, String htmlContent, String textContent) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Email provider is not configured: set BREVO_API_KEY in your environment (.env)");
        }
        if (senderEmail == null || senderEmail.isBlank()) {
            throw new IllegalStateException(
                    "MAIL_SENDER_EMAIL is not set — it must be a sender address verified in your Brevo account");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", senderName, "email", senderEmail));
            body.put("to", List.of(Map.of("email", to)));
            body.put("subject", subject);
            if (htmlContent != null && !htmlContent.isBlank()) body.put("htmlContent", htmlContent);
            if (textContent != null && !textContent.isBlank()) body.put("textContent", textContent);

            String payload = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Brevo API returned HTTP " + response.statusCode() + ": " + response.body());
            }
            log.info("EMAIL_SENT_VIA_BREVO to={} status={}", to, response.statusCode());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email via Brevo: " + e.getMessage(), e);
        }
    }

    // Branded, email-client-safe (table + inline styles) verification template. {{CODE}} is injected.
    private static final String VERIFICATION_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <meta name="color-scheme" content="dark">
            </head>
            <body style="margin:0;padding:0;background-color:#eef0f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#eef0f5;padding:32px 16px;">
                <tr><td align="center">
                  <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background-color:#0b0f1a;border-radius:20px;overflow:hidden;border:1px solid rgba(255,255,255,0.08);">
                    <tr><td style="background-image:linear-gradient(135deg,#8b5cf6,#6366f1 45%,#22d3ee);background-color:#6366f1;padding:26px 32px;">
                      <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                        <td style="vertical-align:middle;padding-right:12px;">
                          <div style="width:42px;height:42px;border-radius:12px;background-color:rgba(255,255,255,0.16);text-align:center;line-height:42px;font-size:22px;font-weight:800;color:#ffffff;">A</div>
                        </td>
                        <td style="vertical-align:middle;">
                          <div style="font-size:20px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">Aurora Bank</div>
                          <div style="font-size:11px;letter-spacing:2px;text-transform:uppercase;color:rgba(255,255,255,0.82);">Digital Banking</div>
                        </td>
                      </tr></table>
                    </td></tr>
                    <tr><td style="padding:36px 32px 8px;">
                      <h1 style="margin:0 0 10px;font-size:24px;color:#ffffff;font-weight:700;">Verify your email</h1>
                      <p style="margin:0 0 26px;font-size:15px;line-height:1.6;color:#9aa3bd;">Use the verification code below to confirm your account and unlock accounts, cards and transfers. The code expires in <strong style="color:#c4b5fd;">15 minutes</strong>.</p>
                      <table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:6px 0 30px;">
                        <div style="display:inline-block;background-image:linear-gradient(135deg,rgba(139,92,246,0.20),rgba(34,211,238,0.12));background-color:#131827;border:1px solid rgba(139,92,246,0.38);border-radius:16px;padding:20px 32px;">
                          <span style="font-size:40px;font-weight:800;letter-spacing:12px;color:#ffffff;font-family:'Courier New',Consolas,monospace;">{{CODE}}</span>
                        </div>
                      </td></tr></table>
                      <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#6b7391;">If you didn't request this, you can safely ignore this email — no changes will be made to your account.</p>
                    </td></tr>
                    <tr><td style="padding:22px 32px 30px;border-top:1px solid rgba(255,255,255,0.08);">
                      <p style="margin:0;font-size:12px;color:#6b7391;">© Aurora Bank · Automated security message, please don't reply.</p>
                    </td></tr>
                  </table>
                  <p style="margin:18px 0 0;font-size:11px;color:#9aa3bd;">Sent securely via Aurora Bank</p>
                </td></tr>
              </table>
            </body>
            </html>
            """;
}
