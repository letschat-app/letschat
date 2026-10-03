package com.letschat.mvp_1.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class BrevoMailService {

    private final WebClient webClient;

    @Value("${brevo.api.key:}")
    private String apiKey;

    @Value("${brevo.sender.email:noreply@letschat.com}")
    private String senderEmail;

    @Value("${brevo.sender.name:LetsChat}")
    private String senderName;

    public BrevoMailService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.brevo.com/v3/smtp/email")
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public Mono<Boolean> sendEmail(String toEmail, String subject, String htmlContent) {
        return sendEmailWithAttachment(toEmail, subject, htmlContent, null, null);
    }

    public Mono<Boolean> sendEmailWithAttachment(String toEmail, String subject, String htmlContent, String fileName, byte[] attachmentBytes) {
        if (!isConfigured()) {
            System.out.println("[BrevoMailService] BREVO_API_KEY not configured. Skipping REST API mail dispatch for " + toEmail);
            return Mono.just(false);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", Map.of("name", senderName, "email", senderEmail));
        payload.put("to", List.of(Map.of("email", toEmail)));
        payload.put("subject", subject);
        payload.put("htmlContent", htmlContent);

        if (fileName != null && attachmentBytes != null && attachmentBytes.length > 0) {
            String base64Content = Base64.getEncoder().encodeToString(attachmentBytes);
            payload.put("attachment", List.of(Map.of("name", fileName, "content", base64Content)));
        }

        return webClient.post()
                .header("api-key", apiKey.trim())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        System.out.println("[BrevoMailService] Email sent via Brevo REST API to " + toEmail);
                        return Mono.just(true);
                    } else {
                        return response.bodyToMono(String.class)
                                .doOnNext(err -> System.err.println("[BrevoMailService] Brevo API Error for " + toEmail + ": " + err))
                                .map(err -> false);
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("[BrevoMailService] Exception during Brevo API call to " + toEmail + ": " + e.getMessage());
                    return Mono.just(false);
                });
    }
}
