package co.com.pedrorido.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/solicitud",
                    method = RequestMethod.POST,
                    beanClass = RequestHandler.class,
                    beanMethod = "listenSaveRequest",
                    operation = @Operation(
                            operationId = "saveSolicitud",
                            summary = "Crear solicitud",
                            security = @SecurityRequirement(name = "bearerAuth")
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitud",
                    method = RequestMethod.GET,
                    beanClass = RequestHandler.class,
                    beanMethod = "listRequests",
                    operation = @Operation(
                            operationId = "listSolicitudes",
                            summary = "Listar solicitudes",
                            security = @SecurityRequirement(name = "bearerAuth")
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(RequestHandler requestHandler) {
        return route(
                POST("/api/v1/solicitud")
                        .and(accept(MediaType.APPLICATION_JSON))
                        .and(contentType(MediaType.APPLICATION_JSON)),
                res -> requestHandler.listenSaveRequest(res)
                        .flatMap(re -> ServerResponse
                                .status(re.getStatusCode())
                                .headers(h -> h.addAll(re.getHeaders()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(re.getBody()))
        ).andRoute(GET("/api/v1/solicitud"), res -> requestHandler.listRequests(res));
    }
}
