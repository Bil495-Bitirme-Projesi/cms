package com.bitiriciler32.cms.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * Swagger UI: /swagger-ui.html
 * Raw OpenAPI JSON: /v3/api-docs
 * Raw OpenAPI YAML: /v3/api-docs.yaml
 *
 * Security schemes:
 *   - userAuth    : Standard user JWT  (Authorization: Bearer <token>)
 *   - subsystemAuth: Subsystem JWT issued by POST /api/auth/subsystem-login
 *                    (same header format, different token payload/scope)
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CMS – Central Management Subsystem API",
                version = "0.1.0",
                description = """
                        REST API for the AI-Powered Automated Security Camera Monitoring Platform.

                        **Authentication**
                        - Most endpoints require a user JWT obtained from `POST /api/auth/login`.
                        - Subsystem endpoints (`/api/events/ingest`) require a subsystem JWT
                          obtained from `POST /api/auth/subsystem-login`.
                        - In both cases the token is passed in the `Authorization: Bearer <token>` header.

                        **Error responses** always use the `ApiErrorResponse` schema:
                        ```json
                        {
                          "timestamp": "2026-01-01T00:00:00Z",
                          "status": 404,
                          "error": "Not Found",
                          "message": "Camera with id 99 not found",
                          "path": "/api/admin/cameras/99"
                        }
                        ```
                        """
        ),
        servers = @Server(url = "/", description = "Current server")
)
@SecurityScheme(
        name = "userAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "User JWT – obtain via POST /api/auth/login"
)
@SecurityScheme(
        name = "subsystemAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Subsystem JWT – obtain via POST /api/auth/subsystem-login"
)
public class OpenApiConfig {
    // Bean-free; all configuration is driven by annotations above.
}



