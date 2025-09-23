package co.com.pedrorido.api;

import co.com.pedrorido.api.dto.CreateRequestDTO;
import co.com.pedrorido.api.dto.GeneralResponseDTO;
import co.com.pedrorido.api.dto.UpdateRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
                            summary = "Crear solicitud - CLIENTE",
                            security = @SecurityRequirement(name = "bearerAuth"),
                            tags = {"Solicitudes"},
                            description = "Recibe un objeto CreateRequestDTO en el cuerpo de la solicitud, lo procesa y guarda la solicitud. Devuelve la información de la solicitud guardada junto con un mensaje de éxito.",
                            requestBody = @RequestBody(
                                    description = "Información de la solicitud a guardar",
                                    required = true,
                                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateRequestDTO.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "La solicitud se agregó correctamente.",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralResponseDTO.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "El cuerpo de la solicitud es inválido.",
                                            content = @Content
                                    ),
                                    @ApiResponse(
                                            responseCode = "409",
                                            description = "Fallo funcional.",
                                            content = @Content
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitud",
                    method = RequestMethod.GET,
                    beanClass = RequestHandler.class,
                    beanMethod = "listRequests",
                    operation = @Operation(
                            operationId = "listSolicitudes",
                            summary = "Listar solicitudes - ADMIN",
                            security = @SecurityRequirement(name = "bearerAuth"),
                            tags = {"Solicitudes"},
                            description = "Obtiene una lista de solicitudes basándose en los parámetros proporcionados en la consulta. Devuelve los detalles de las solicitudes encontradas o un mensaje de error si ocurre algún problema.",
                            method = "GET",
                            parameters = {
                                    @Parameter(
                                            name = "page",
                                            description = "Número de página para la paginación (opcional)",
                                            in = ParameterIn.QUERY,
                                            required = false,
                                            schema = @Schema(type = "integer", defaultValue = "0")
                                    ),
                                    @Parameter(
                                            name = "size",
                                            description = "Tamaño de página para la paginación (opcional)",
                                            in = ParameterIn.QUERY,
                                            required = false,
                                            schema = @Schema(type = "integer", defaultValue = "10")
                                    ),
                                    @Parameter(
                                            name = "estado",
                                            description = "Estado de solicitudes para filtrar la paginación",
                                            in = ParameterIn.QUERY,
                                            required = false,
                                            schema = @Schema(type = "integer", defaultValue = "1")
                                    )
                            },
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Lista de solicitudes obtenida con éxito.",
                                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralResponseDTO.class))
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Parámetros de consulta inválidos.",
                                            content = @Content
                                    ),
                                    @ApiResponse(
                                            responseCode = "500",
                                            description = "Error interno del servidor.",
                                            content = @Content
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitud",
                    method = RequestMethod.PUT,
                    beanClass = RequestHandler.class,
                    beanMethod = "listenUpdateRequestStatus",
                    operation = @Operation(
                            operationId = "listenUpdateRequestStatus",
                            summary = "Actualiza el estado de una solicitud - ASESOR", // Breve descripción del método
                            description = "Este endpoint permite actualizar el estado de una solicitud según los datos proporcionados en el cuerpo de la petición.", // Descripción más detallada
                            security = @SecurityRequirement(name = "bearerAuth"),
                            tags = {"Solicitudes"}, // Categoriza el endpoint
                            requestBody = @RequestBody(
                                    description = "Representa la información necesaria para actualizar el estado de la solicitud.",
                                    required = true,
                                    content = @Content(
                                            schema = @Schema(implementation = UpdateRequestDTO.class) // Haz referencia al DTO esperado
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Respuesta exitosa con los detalles actualizados de la solicitud.",
                                            content = @Content(
                                                    schema = @Schema(implementation = GeneralResponseDTO.class) // Esquemático para la respuesta
                                            )
                                    ),
                                    @ApiResponse(responseCode = "400", description = "Solicitud incorrecta. Verifique los datos enviados."),
                                    @ApiResponse(responseCode = "404", description = "No se encontró la solicitud especificada."),
                                    @ApiResponse(responseCode = "500", description = "Error interno del servidor.")
                            }

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
                                .bodyValue(re.getBody())))
                .andRoute(GET("/api/v1/solicitud"), res -> requestHandler.listRequests(res))
                .andRoute(PUT("/api/v1/solicitud")
                        .and(accept(MediaType.APPLICATION_JSON))
                        .and(contentType(MediaType.APPLICATION_JSON)), res -> requestHandler.listenUpdateRequestStatus(res)
                        .flatMap(re -> ServerResponse
                                .status(re.getStatusCode())
                                .headers(h -> h.addAll(re.getHeaders()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(re.getBody())));
    }
}
