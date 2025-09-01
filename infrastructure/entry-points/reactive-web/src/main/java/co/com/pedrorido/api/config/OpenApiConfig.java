package co.com.pedrorido.api.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Requests Microservice")
                        .description("Api Rest to start requests (WebFlux and Hexagonal Architecture)")
                        .version("v1.0.0"))
                .externalDocs(new ExternalDocumentation()
                        .description("Project documentation")
                        .url("https://github.com/ridom997/pedro-rios-crediya-requests-ms"));
    }
}
