package com.letschat.mvp_1;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class R2configuration {

    @Value("${r2.accessKey}")
    private String accessKey;

    @Value("${r2.secretKey}")
    private String secretKey;

    @Value("${r2.endpoint}")
    private String endpoint;

    @Bean
    public S3Presigner s3Presigner() {

        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(accessKey, secretKey);

        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .build();
    }
}