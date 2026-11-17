package com.vaultai.be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vaultAiOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Vault AI API")
                .version("v1")
                .description("""
                        REST API of the Vault AI backend.

                        Authentication: sign in through the application first. The session cookie \
                        (VAULT-SESSION) is sent automatically and Swagger UI adds the CSRF header.
                        """));
    }
}