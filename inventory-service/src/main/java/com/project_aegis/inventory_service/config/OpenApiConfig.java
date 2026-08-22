package com.project_aegis.inventory_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8083}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String bearerAuthScheme = "Bearer Authentication";
        final String internalApiKeyScheme = "Internal API Key";

        return new OpenAPI()
                .info(new Info()
                        .title("Inventory & Flash Sale Campaign Service API")
                        .version("1.0.0")
                        .description("Production-grade Inventory Service for Flash Sale Campaigns, SKU allocations, and Order Service stock reservations.")
                        .contact(new Contact()
                                .name("Project Aegis Team")
                                .email("dev@project-aegis.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Environment"),
                        new Server().url("/").description("API Gateway / Default Server")))
                .addSecurityItem(new SecurityRequirement().addList(bearerAuthScheme))
                .components(new Components()
                        .addSecuritySchemes(bearerAuthScheme, new SecurityScheme()
                                .name(bearerAuthScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter Keycloak JWT Bearer token"))
                        .addSecuritySchemes(internalApiKeyScheme, new SecurityScheme()
                                .name("X-Internal-Api-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Internal service-to-service communication key")));
    }
}
