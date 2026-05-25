package myexpressionfriend_api.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.server-url:}")
    private String swaggerServerUrl;

    @Bean
    public OpenAPI openAPI() {
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
                        .description("JWT 액세스 토큰을 입력합니다. 예: Bearer {accessToken}"));

        return new OpenAPI()
                .info(apiInfo())
                .servers(swaggerServers())
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    private List<Server> swaggerServers() {
        if (swaggerServerUrl == null || swaggerServerUrl.isBlank()) {
            return List.of();
        }

        return List.of(new Server()
                .url(swaggerServerUrl)
                .description("API server"));
    }

    private Info apiInfo() {
        return new Info()
                .title("나의 표정 친구 API")
                .description("가정과 치료 환경에서 아동의 표정 인식과 사회적 의사소통 학습을 지원하는 AR 기반 서비스 API")
                .version("1.0.0")
                .contact(new Contact()
                        .name("PlanB Team")
                        .email("contact@example.com"));
    }
}
