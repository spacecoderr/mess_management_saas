package com.thali.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/** Swagger UI at /swagger-ui.html. Use the "Authorize" button and paste an access token. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI thaliOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Thali - IIIT Bhubaneswar Mess API").version("v1")
                        .description("Mess management: auth, menus, bookings, attendance, feedback, complaints, billing, analytics."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
