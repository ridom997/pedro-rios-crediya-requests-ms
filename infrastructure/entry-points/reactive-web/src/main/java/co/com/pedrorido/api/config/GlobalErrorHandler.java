package co.com.pedrorido.api.config;

import co.com.pedrorido.api.dto.GeneralResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Component
@Order(-2)
public class GlobalErrorHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();

        if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof IllegalStateException) {
            status = HttpStatus.CONFLICT;
        }

        HashMap<String, String> data = new HashMap<>();
        data.put("uri", exchange.getRequest().getPath().value());
        var errorDTO = GeneralResponseDTO.<String>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();

        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = null;
        try {
            bytes = new ObjectMapper().writeValueAsBytes(errorDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        var buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}