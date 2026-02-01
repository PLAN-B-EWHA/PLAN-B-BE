package com.planB.myexpressionfriend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * Swagger (OpenAPI 3.0) 설정
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // Security Scheme 정의 (JWT)
        String jwtSchemeName = "JWT Bearer Token";
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);

        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("JWT Access Token"));

        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    private Info apiInfo() {
        return new Info()
                .title("나의표정친구 API")
                .description("자폐 스펙트럼 장애 아동을 위한 AR 기반 표정 훈련 애플리케이션 API")
                .version("1.0.0")
                .contact(new Contact()
                        .name("PlanB Team")
                        .email("contact@example.com"));
    }

}
