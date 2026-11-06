package com.openbas.intelligence.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("OpenBAS Intelligence API")
                .description("Industrial Interoperability and Agentic Building Operations Platform. " +
                    "All endpoints require JWT authentication except /api/v1/auth/login. " +
                    "AI agents cannot directly execute commands — all commands require policy validation and human approval.")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("OpenBAS Intelligence")
                    .email("admin@openbas.local"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token obtained from /api/v1/auth/login")));
    }
}
