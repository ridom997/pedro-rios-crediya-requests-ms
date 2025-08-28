package co.com.pedrorido.api;

import co.com.pedrorido.api.dto.CreateRequestDTO;
import co.com.pedrorido.api.dto.GeneralResponseDTO;
import co.com.pedrorido.api.dto.RequestResponseDTO;
import co.com.pedrorido.api.mapper.CreateRequestDTOMapper;
import co.com.pedrorido.api.mapper.RequestDTOMapper;
import co.com.pedrorido.usecase.request.RequestUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.HashMap;

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
                    data.put("user", savedRequestDto);
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
}
