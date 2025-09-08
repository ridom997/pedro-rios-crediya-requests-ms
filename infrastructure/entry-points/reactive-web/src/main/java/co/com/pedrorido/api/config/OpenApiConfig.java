package co.com.pedrorido.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        var bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        return new OpenAPI()
                .info(new Info()
                        .title("Requests Microservice")
                        .description("Api Rest to start requests (WebFlux and Hexagonal Architecture)")
                        .version("v1.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth", bearer))
                .externalDocs(new ExternalDocumentation()
                        .description("Project documentation")
                        .url("https://github.com/ridom997/pedro-rios-crediya-requests-ms"));
    }
}
