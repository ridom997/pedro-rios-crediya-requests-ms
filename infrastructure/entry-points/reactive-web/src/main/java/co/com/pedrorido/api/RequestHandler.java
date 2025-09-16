package co.com.pedrorido.api;

import co.com.pedrorido.api.dto.CreateRequestDTO;
import co.com.pedrorido.api.dto.GeneralResponseDTO;
import co.com.pedrorido.api.dto.RequestResponseDTO;
import co.com.pedrorido.api.dto.UpdateRequestDTO;
import co.com.pedrorido.api.mapper.CreateRequestDTOMapper;
import co.com.pedrorido.api.mapper.RequestDTOMapper;
import co.com.pedrorido.model.status.Status;
import co.com.pedrorido.model.utils.StatusEnum;
import co.com.pedrorido.usecase.request.RequestUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Component
@RequiredArgsConstructor
@Log4j2
public class RequestHandler {
    private final CreateRequestDTOMapper createRequestDTOMapper;
    private final RequestDTOMapper requestDTOMapper;
    private final RequestUseCase requestUseCase;
    private final TransactionalOperator tx;

    @Operation(
            summary = "Guardar solicitud",
            description = "Recibe un objeto CreateRequestDTO en el cuerpo de la solicitud, lo procesa y guarda la solicitud. Devuelve la información de la solicitud guardada junto con un mensaje de éxito.",
            security = @SecurityRequirement(name = "bearerAuth"),
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
    public Mono<ResponseEntity<GeneralResponseDTO<RequestResponseDTO>>> listenSaveRequest(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CreateRequestDTO.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Request body is required")))
                .doOnNext(log::info)
                .map(createRequestDTOMapper::toDomain)
                .flatMap(createRequestDTO -> requestUseCase.createRequest(createRequestDTO).as(tx::transactional))
                .map(requestDTOMapper::toDto)
                .map(savedRequestDto -> {
                    HashMap<String, RequestResponseDTO> data = new HashMap<>();
                    data.put("request", savedRequestDto);
                    return new ResponseEntity<>(
                            GeneralResponseDTO.<RequestResponseDTO>builder()
                                    .success(true)
                                    .message("Request added successfully")
                                    .data(data)
                                    .build(),
                            HttpStatus.CREATED);
                })
                .doOnSuccess(log::info)
                .doOnError(log::error);
    }


    @Operation(
            summary = "Listar solicitudes",
            description = "Obtiene una lista de solicitudes basándose en los parámetros proporcionados en la consulta. Devuelve los detalles de las solicitudes encontradas o un mensaje de error si ocurre algún problema.",
            security = @SecurityRequirement(name = "bearerAuth"),
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
    public Mono<ServerResponse> listRequests(ServerRequest req) {

        int page = parseIntOrDefault(req.queryParam("page").orElse("0"), 0);
        int size = parseIntOrDefault(req.queryParam("size").orElse("20"), 10);

        // estado puede venir como ?estado=PENDIENTE&estado=APROBADA o ?estado=PENDIENTE,APROBADA
        Set<Long> status = req.queryParams().getOrDefault("estado", List.of(StatusEnum.PENDING.getId().toString()))
                .stream()
                .flatMap(v -> stream(v.split(",")))
                .filter(s -> !s.isBlank())
                .map(Long::valueOf)
                .collect(Collectors.toSet());

        return requestUseCase.getListByStatus(status, page, size)
                .flatMap(dto -> org.springframework.web.reactive.function.server.ServerResponse.ok().bodyValue(dto));
    }

    private static int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public Mono<ResponseEntity<GeneralResponseDTO<RequestResponseDTO>>> listenUpdateRequestStatus(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(UpdateRequestDTO.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Request body is required")))
                .doOnNext(log::info)
                .flatMap(updateRequest -> requestUseCase.updateStatusRequest(updateRequest.getId(), updateRequest.getStatusId()).as(tx::transactional))
                .map(requestDTOMapper::toDto)
                .map(updatedRequestDto -> {
                    HashMap<String, RequestResponseDTO> data = new HashMap<>();
                    data.put("request", updatedRequestDto);
                    return new ResponseEntity<>(
                            GeneralResponseDTO.<RequestResponseDTO>builder()
                                    .success(true)
                                    .message("Request updated successfully")
                                    .data(data)
                                    .build(),
                            HttpStatus.ACCEPTED);
                })
                .doOnSuccess(log::info)
                .doOnError(log::error);
    }
}
