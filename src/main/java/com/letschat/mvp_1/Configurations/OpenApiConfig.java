package com.letschat.mvp_1.Configurations;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Server prodServer = new Server()
                .url("https://letschat-5dxg.onrender.com")
                .description("Production Server (Render)");

        Server localServer = new Server()
                .url("http://localhost:10000")
                .description("Local Development Server");

        Contact contact = new Contact()
                .name("LetsChat Engineering Team")
                .email("noreply@letschat.com");

        Info info = new Info()
                .title("LetsChat MVP Reactive REST & WebSocket API")
                .version("v1.0.0")
                .description("Interactive API Documentation for Pulses, Native Polls, User Profiles, 2FA Authentication, Media Storage, Classrooms, and Virtual Rooms.")
                .contact(contact);

        SecurityScheme userIdHeaderScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("User-Id")
                .description("User-Id authentication header (e.g. 'AAA001')");

        return new OpenAPI()
                .info(info)
                .servers(List.of(prodServer, localServer))
                .components(new Components().addSecuritySchemes("User-Id", userIdHeaderScheme))
                .addSecurityItem(new SecurityRequirement().addList("User-Id"));
    }
}
