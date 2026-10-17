package com.letschat.mvp_1.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class OtpService {

    @Autowired
    private BrevoMailService brevoMailService;

    @Value("${letschat.app.logo-url:https://raw.githubusercontent.com/letschat-app/letschat-test-app/main/public/icon.png}")
    private String appLogoUrl;

    private static final long OTP_VALIDITY_SECONDS = 600; // 10 minutes
    private static final long RESET_TOKEN_VALIDITY_SECONDS = 900; // 15 minutes
    private final SecureRandom random = new SecureRandom();

    private final Map<String, OtpEntry> registrationOtpMap = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> forgotPasswordOtpMap = new ConcurrentHashMap<>();
    private final Map<String, ResetTokenEntry> passwordResetTokenMap = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> passwordChangeOtpMap = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> twoFactorOtpMap = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> twoFactorConfigOtpMap = new ConcurrentHashMap<>();

    private String getLogoHtml() {
        if (appLogoUrl == null || appLogoUrl.trim().isEmpty()) {
            return "";
        }
        return """
            <div style="text-align: center; margin-bottom: 20px;">
                <img src="%s" alt="LetsChat" width="64" height="64" style="width: 64px; height: 64px; border-radius: 14px; display: inline-block; object-fit: cover;" />
            </div>
            """.formatted(appLogoUrl.trim());
    }

    private static class OtpEntry {
        final String otp;
        final Instant expiresAt;

        OtpEntry(String otp, Instant expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }

        boolean isValid(String inputOtp) {
            return this.otp.equals(inputOtp) && Instant.now().isBefore(expiresAt);
        }
    }

    private static class ResetTokenEntry {
        final String token;
        final Instant expiresAt;

        ResetTokenEntry(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        boolean isValid(String inputToken) {
            return this.token.equals(inputToken) && Instant.now().isBefore(expiresAt);
        }
    }

    private String generate6DigitOtp() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public Mono<Boolean> sendRegistrationOtp(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Mono.just(false);
        }
        String cleanEmail = email.trim().toLowerCase();
        String otp = generate6DigitOtp();
        registrationOtpMap.put(cleanEmail, new OtpEntry(otp, Instant.now().plusSeconds(OTP_VALIDITY_SECONDS)));

        String logoHtml = getLogoHtml();
        String subject = "LetsChat - Registration Verification Code";
        String html = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f8fafc;">
                <div style="max-width: 500px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 16px; padding: 32px 24px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                    %s
                    <h2 style="color: #2563eb; text-align: center; margin: 0 0 8px 0; font-size: 22px;">Welcome to LetsChat!</h2>
                    <p style="text-align: center; color: #64748b; font-size: 14px; margin: 0 0 24px 0;">Use the following 6-digit verification code to complete your registration:</p>
                    <div style="text-align: center; margin: 24px 0;">
                        <span style="font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #0f172a; background: #eff6ff; border: 1px solid #dbeafe; padding: 12px 28px; border-radius: 12px; display: inline-block;">%s</span>
                    </div>
                    <div style="text-align: center; margin-top: 24px; padding-top: 16px; border-top: 1px solid #f1f5f9;">
                        <p style="font-size: 12px; color: #94a3b8; margin: 0;">This OTP is valid for 10 minutes. If you did not request this, please ignore this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(logoHtml, otp);

        return sendMail(cleanEmail, subject, html);
    }

    public boolean verifyRegistrationOtp(String email, String inputOtp) {
        if (email == null || inputOtp == null) return false;
        String cleanEmail = email.trim().toLowerCase();
        OtpEntry entry = registrationOtpMap.get(cleanEmail);
        if (entry != null && entry.isValid(inputOtp.trim())) {
            registrationOtpMap.remove(cleanEmail);
            return true;
        }
        return false;
    }

    public Mono<Boolean> sendForgotPasswordOtp(String identifier, String targetEmail) {
        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            return Mono.just(false);
        }
        String cleanKey = identifier.trim().toLowerCase();
        String otp = generate6DigitOtp();
        forgotPasswordOtpMap.put(cleanKey, new OtpEntry(otp, Instant.now().plusSeconds(OTP_VALIDITY_SECONDS)));

        String logoHtml = getLogoHtml();
        String subject = "LetsChat - Password Reset Code";
        String html = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f8fafc;">
                <div style="max-width: 500px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 16px; padding: 32px 24px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                    %s
                    <h2 style="color: #dc2626; text-align: center; margin: 0 0 8px 0; font-size: 22px;">Password Reset Request</h2>
                    <p style="text-align: center; color: #64748b; font-size: 14px; margin: 0 0 24px 0;">We received a request to reset your password. Use the code below to complete the reset process:</p>
                    <div style="text-align: center; margin: 24px 0;">
                        <span style="font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #0f172a; background: #fef2f2; border: 1px solid #fee2e2; padding: 12px 28px; border-radius: 12px; display: inline-block;">%s</span>
                    </div>
                    <div style="text-align: center; margin-top: 24px; padding-top: 16px; border-top: 1px solid #f1f5f9;">
                        <p style="font-size: 12px; color: #94a3b8; margin: 0;">This code expires in 10 minutes. If you did not request a password reset, your account is safe.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(logoHtml, otp);

        return sendMail(targetEmail.trim().toLowerCase(), subject, html);
    }

    public boolean verifyForgotPasswordOtp(String identifier, String inputOtp) {
        if (identifier == null || inputOtp == null) return false;
        String cleanKey = identifier.trim().toLowerCase();
        OtpEntry entry = forgotPasswordOtpMap.get(cleanKey);
        if (entry != null && entry.isValid(inputOtp.trim())) {
            forgotPasswordOtpMap.remove(cleanKey);
            return true;
        }
        return false;
    }

    public String verifyForgotPasswordOtpAndIssueToken(String identifier, String inputOtp) {
        if (identifier == null || inputOtp == null) return null;
        String cleanKey = identifier.trim().toLowerCase();
        OtpEntry entry = forgotPasswordOtpMap.get(cleanKey);
        if (entry != null && entry.isValid(inputOtp.trim())) {
            forgotPasswordOtpMap.remove(cleanKey);
            String token = UUID.randomUUID().toString();
            passwordResetTokenMap.put(cleanKey, new ResetTokenEntry(token, Instant.now().plusSeconds(RESET_TOKEN_VALIDITY_SECONDS)));
            return token;
        }
        return null;
    }

    public boolean verifyAndConsumeResetToken(String identifier, String inputToken) {
        if (identifier == null || inputToken == null) return false;
        String cleanKey = identifier.trim().toLowerCase();
        ResetTokenEntry entry = passwordResetTokenMap.get(cleanKey);
        if (entry != null && entry.isValid(inputToken.trim())) {
            passwordResetTokenMap.remove(cleanKey);
            return true;
        }
        return false;
    }

    public Mono<Boolean> send2FAOtp(String identifier, String targetEmail) {
        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            return Mono.just(false);
        }
        String cleanKey = identifier.trim().toLowerCase();
        String otp = generate6DigitOtp();
        twoFactorOtpMap.put(cleanKey, new OtpEntry(otp, Instant.now().plusSeconds(OTP_VALIDITY_SECONDS)));

        String logoHtml = getLogoHtml();
        String subject = "LetsChat - 2-Factor Authentication Code";
        String html = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f8fafc;">
                <div style="max-width: 500px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 16px; padding: 32px 24px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                    %s
                    <h2 style="color: #059669; text-align: center; margin: 0 0 8px 0; font-size: 22px;">2FA Login Verification</h2>
                    <p style="text-align: center; color: #64748b; font-size: 14px; margin: 0 0 24px 0;">Use the following 6-digit code to complete your login:</p>
                    <div style="text-align: center; margin: 24px 0;">
                        <span style="font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #0f172a; background: #ecfdf5; border: 1px solid #d1fae5; padding: 12px 28px; border-radius: 12px; display: inline-block;">%s</span>
                    </div>
                    <div style="text-align: center; margin-top: 24px; padding-top: 16px; border-top: 1px solid #f1f5f9;">
                        <p style="font-size: 12px; color: #94a3b8; margin: 0;">This code expires in 10 minutes. If you did not initiate this login, please change your password immediately.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(logoHtml, otp);

        return sendMail(targetEmail.trim().toLowerCase(), subject, html);
    }

    public boolean verify2FAOtp(String identifier, String inputOtp) {
        if (identifier == null || inputOtp == null) return false;
        String cleanKey = identifier.trim().toLowerCase();
        OtpEntry entry = twoFactorOtpMap.get(cleanKey);
        if (entry != null && entry.isValid(inputOtp.trim())) {
            twoFactorOtpMap.remove(cleanKey);
            return true;
        }
        return false;
    }

    public Mono<Boolean> sendWelcomeEmail(String email, String username, String userId) {
        if (email == null || email.trim().isEmpty()) {
            return Mono.just(false);
        }
        String cleanEmail = email.trim().toLowerCase();
        String safeUsername = (username != null && !username.trim().isEmpty()) ? username.trim() : "Friend";
        String safeUserId = (userId != null && !userId.trim().isEmpty()) ? userId.trim() : "N/A";

        String logoHtml = getLogoHtml();
        String subject = "Welcome to LetsChat, " + safeUsername + "! 🎉 Your Account is Ready";
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #1e293b; margin: 0; padding: 20px; background-color: #f8fafc;">
                <div style="max-width: 520px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 16px; padding: 36px 28px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                    %s
                    <h2 style="color: #0f172a; text-align: center; margin: 0 0 8px 0; font-size: 24px; font-weight: 700;">Welcome to LetsChat, %s! 🎉</h2>
                    <p style="text-align: center; color: #64748b; font-size: 14px; margin: 0 0 24px 0;">Thank you for registering with LetsChat. Your account has been verified and is ready to use.</p>
                    
                    <!-- USER ID & CREDENTIAL CARD -->
                    <div style="background-color: #eff6ff; border: 1px solid #bfdbfe; border-radius: 12px; padding: 20px; text-align: center; margin: 20px 0;">
                        <div style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.5px; color: #2563eb; margin-bottom: 8px;">Your Unique User ID</div>
                        <div style="font-size: 30px; font-weight: 800; letter-spacing: 4px; color: #1e3a8a; font-family: monospace; background-color: #ffffff; padding: 8px 20px; border-radius: 8px; border: 1px solid #dbeafe; display: inline-block;">
                            %s
                        </div>
                        <div style="margin-top: 12px; font-size: 13px; color: #475569;">
                            Username: <strong style="color: #0f172a;">@%s</strong>
                        </div>
                    </div>

                    <!-- GUIDANCE SECTION (Table for 100% email client compatibility) -->
                    <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="width: 100%; margin: 24px 0 16px 0;">
                        <tr>
                            <td style="width: 36px; vertical-align: top; font-size: 22px; line-height: 1; padding-top: 2px;">🔑</td>
                            <td style="vertical-align: top; font-size: 14px; color: #334155; line-height: 1.5; padding-bottom: 16px;">
                                <strong style="color: #0f172a;">Easy Login:</strong> You can use your <strong>User ID</strong> (<code style="background: #f1f5f9; padding: 2px 6px; border-radius: 4px; color: #2563eb; font-weight: 600;">%s</code>) or your <strong>Username</strong> (<code style="background: #f1f5f9; padding: 2px 6px; border-radius: 4px; color: #0f172a; font-weight: 600;">@%s</code>) along with your password to log in.
                            </td>
                        </tr>
                        <tr>
                            <td style="width: 36px; vertical-align: top; font-size: 22px; line-height: 1; padding-top: 2px;">💬</td>
                            <td style="vertical-align: top; font-size: 14px; color: #334155; line-height: 1.5;">
                                <strong style="color: #0f172a;">Share &amp; Connect:</strong> Share your <strong>User ID</strong> (<code style="background: #f1f5f9; padding: 2px 6px; border-radius: 4px; color: #2563eb; font-weight: 600;">%s</code>) with friends, classmates, or colleagues so they can search for you and start chatting with you immediately on LetsChat!
                            </td>
                        </tr>
                    </table>

                    <!-- FOOTER -->
                    <div style="text-align: center; margin-top: 28px; padding-top: 18px; border-top: 1px solid #f1f5f9;">
                        <p style="font-size: 12px; color: #94a3b8; margin: 0 0 4px 0;">© 2026 LetsChat Inc. All rights reserved.</p>
                        <p style="font-size: 11px; color: #cbd5e1; margin: 0;">Fast, secure real-time messaging.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(logoHtml, safeUsername, safeUserId, safeUsername, safeUserId, safeUsername, safeUserId);

        return sendMail(cleanEmail, subject, html);
    }

    public Mono<Boolean> sendPasswordChangeOtp(String userId, String targetEmail) {
        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            return Mono.just(false);
        }
        String cleanKey = userId.trim().toLowerCase();
        String otp = generate6DigitOtp();
        passwordChangeOtpMap.put(cleanKey, new OtpEntry(otp, Instant.now().plusSeconds(OTP_VALIDITY_SECONDS)));

        String logoHtml = getLogoHtml();
        String subject = "LetsChat - Password Change Verification Code";
        String html = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f8fafc;">
                <div style="max-width: 500px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 16px; padding: 32px 24px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                    %s
                    <h2 style="color: #ea580c; text-align: center; margin: 0 0 8px 0; font-size: 22px;">Password Change Request</h2>
                    <p style="text-align: center; color: #64748b; font-size: 14px; margin: 0 0 24px 0;">We received a request to change your LetsChat password. Because Two-Factor Authentication is enabled on your account, please enter this verification code to confirm:</p>
                    <div style="text-align: center; margin: 24px 0;">
                        <span style="font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #0f172a; background: #fff7ed; border: 1px solid #ffedd5; padding: 12px 28px; border-radius: 12px; display: inline-block;">%s</span>
                    </div>
                    <div style="text-align: center; margin-top: 24px; padding-top: 16px; border-top: 1px solid #f1f5f9;">
                        <p style="font-size: 12px; color: #94a3b8; margin: 0;">This code expires in 10 minutes. If you did not initiate this change, please contact support immediately.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(logoHtml, otp);

        return sendMail(targetEmail.trim().toLowerCase(), subject, html);
    }

    public boolean verifyPasswordChangeOtp(String userId, String inputOtp) {
        if (userId == null || inputOtp == null) return false;
        String cleanKey = userId.trim().toLowerCase();
        OtpEntry entry = passwordChangeOtpMap.get(cleanKey);
        if (entry != null && entry.isValid(inputOtp.trim())) {
            passwordChangeOtpMap.remove(cleanKey);
            return true;
        }
        return false;
    }

    public Mono<Boolean> send2FAConfigOtp(String userId, String targetEmail, boolean enable) {
        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            return Mono.just(false);
        }
        String cleanKey = userId.trim().toLowerCase();
        String otp = generate6DigitOtp();
        twoFactorConfigOtpMap.put(cleanKey, new OtpEntry(otp, Instant.now().plusSeconds(OTP_VALIDITY_SECONDS)));

        String actionText = enable ? "Enable" : "Disable";
        String actionBadgeColor = enable ? "#059669" : "#dc2626";
        String bgLight = enable ? "#ecfdf5" : "#fef2f2";
        String borderLight = enable ? "#d1fae5" : "#fee2e2";

        String logoHtml = getLogoHtml();
        String subject = "LetsChat - 2FA Security Verification Code";
        String html = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f8fafc;">
                <div style="max-width: 500px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 16px; padding: 32px 24px; background-color: #ffffff; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                    %s
                    <h2 style="color: %s; text-align: center; margin: 0 0 8px 0; font-size: 22px;">Confirm 2FA %s</h2>
                    <p style="text-align: center; color: #64748b; font-size: 14px; margin: 0 0 24px 0;">You requested to <strong>%s</strong> Two-Factor Authentication on your LetsChat account. Use this 6-digit code to complete the verification:</p>
                    <div style="text-align: center; margin: 24px 0;">
                        <span style="font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #0f172a; background: %s; border: 1px solid %s; padding: 12px 28px; border-radius: 12px; display: inline-block;">%s</span>
                    </div>
                    <div style="text-align: center; margin-top: 24px; padding-top: 16px; border-top: 1px solid #f1f5f9;">
                        <p style="font-size: 12px; color: #94a3b8; margin: 0;">This code expires in 10 minutes. If you did not make this request, please change your password immediately.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(logoHtml, actionBadgeColor, actionText, actionText.toLowerCase(), bgLight, borderLight, otp);

        return sendMail(targetEmail.trim().toLowerCase(), subject, html);
    }

    public boolean verify2FAConfigOtp(String userId, String inputOtp) {
        if (userId == null || inputOtp == null) return false;
        String cleanKey = userId.trim().toLowerCase();
        OtpEntry entry = twoFactorConfigOtpMap.get(cleanKey);
        if (entry != null && entry.isValid(inputOtp.trim())) {
            twoFactorConfigOtpMap.remove(cleanKey);
            return true;
        }
        return false;
    }

    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void cleanExpiredOtps() {
        Instant now = Instant.now();
        registrationOtpMap.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
        forgotPasswordOtpMap.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
        passwordResetTokenMap.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
        passwordChangeOtpMap.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
        twoFactorOtpMap.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
        twoFactorConfigOtpMap.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt));
    }

    private Mono<Boolean> sendMail(String toEmail, String subject, String htmlContent) {
        if (brevoMailService != null && brevoMailService.isConfigured()) {
            return brevoMailService.sendEmail(toEmail, subject, htmlContent);
        }
        System.out.println("[OtpService] BREVO_API_KEY is not configured in environment. Skipping email dispatch to: " + toEmail + ", Subject: " + subject);
        return Mono.just(false);
    }
}
