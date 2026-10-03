package com.letschat.mvp_1.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class OtpService {

    @Autowired
    private BrevoMailService brevoMailService;

    private static final long OTP_VALIDITY_SECONDS = 600; // 10 minutes
    private final SecureRandom random = new SecureRandom();

    private final Map<String, OtpEntry> registrationOtpMap = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> forgotPasswordOtpMap = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> twoFactorOtpMap = new ConcurrentHashMap<>();

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

        String subject = "LetsChat - Registration Verification Code";
        String html = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 500px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; padding: 20px;">
                    <h2 style="color: #4A90E2; text-align: center;">Welcome to LetsChat!</h2>
                    <p>Use the following 6-digit verification code to complete your registration:</p>
                    <div style="text-align: center; margin: 20px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #2C3E50; background: #F2F4F4; padding: 10px 20px; border-radius: 6px;">%s</span>
                    </div>
                    <p style="font-size: 0.9em; color: #777;">This OTP is valid for 10 minutes. If you did not request this, please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(otp);

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

        String subject = "LetsChat - Password Reset Code";
        String html = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 500px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; padding: 20px;">
                    <h2 style="color: #E74C3C; text-align: center;">Password Reset Request</h2>
                    <p>We received a request to reset your password. Use the code below to complete the reset process:</p>
                    <div style="text-align: center; margin: 20px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #2C3E50; background: #FDEDEC; padding: 10px 20px; border-radius: 6px;">%s</span>
                    </div>
                    <p style="font-size: 0.9em; color: #777;">This code expires in 10 minutes. If you did not request a password reset, your account is safe.</p>
                </div>
            </body>
            </html>
            """.formatted(otp);

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

    public Mono<Boolean> send2FAOtp(String identifier, String targetEmail) {
        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            return Mono.just(false);
        }
        String cleanKey = identifier.trim().toLowerCase();
        String otp = generate6DigitOtp();
        twoFactorOtpMap.put(cleanKey, new OtpEntry(otp, Instant.now().plusSeconds(OTP_VALIDITY_SECONDS)));

        String subject = "LetsChat - 2-Factor Authentication Code";
        String html = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 500px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; padding: 20px;">
                    <h2 style="color: #27AE60; text-align: center;">2FA Login Verification</h2>
                    <p>Use the following 6-digit code to complete your login:</p>
                    <div style="text-align: center; margin: 20px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #2C3E50; background: #E8F8F5; padding: 10px 20px; border-radius: 6px;">%s</span>
                    </div>
                    <p style="font-size: 0.9em; color: #777;">This code expires in 10 minutes. If you did not initiate this login, please change your password immediately.</p>
                </div>
            </body>
            </html>
            """.formatted(otp);

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

    private Mono<Boolean> sendMail(String toEmail, String subject, String htmlContent) {
        if (brevoMailService != null && brevoMailService.isConfigured()) {
            return brevoMailService.sendEmail(toEmail, subject, htmlContent);
        }
        System.out.println("[OtpService] BREVO_API_KEY is not configured in environment. Skipping email dispatch to: " + toEmail + ", Subject: " + subject);
        return Mono.just(false);
    }
}
