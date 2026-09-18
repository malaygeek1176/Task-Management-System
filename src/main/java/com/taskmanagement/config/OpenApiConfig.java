package com.taskmanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Configures the OpenAPI/Swagger document metadata and registers a "bearerAuth"
 * security scheme so protected endpoints can be authorized (and tested) directly
 * from Swagger UI at {@code /swagger-ui.html} using a JWT obtained from
 * {@code POST /api/auth/login}.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Task Management Application API",
                version = "1.0.0",
                description = "REST API for registering users, authenticating, and creating, "
                        + "assigning, and managing tasks."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
