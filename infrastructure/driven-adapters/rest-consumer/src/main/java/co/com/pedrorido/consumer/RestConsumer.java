package co.com.pedrorido.consumer;

import co.com.pedrorido.model.external.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Log4j2
public class RestConsumer implements UserRepository {
    private final WebClient client;

    @Override
    public Mono<Boolean> userExistsByDocumentNumber(String documentNumber) {
        log.info("WebClient - userExistsByDocumentNumber: {}", documentNumber);
        return client.get()
                .uri(uri -> uri.path("/api/v1/users/exist")
                        .queryParam("documentNumber", documentNumber)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<GeneralResponseDTO<Boolean>>() {})
                .map(resp -> resp != null && resp.getData() != null && resp.getData().get("userExists"))
                .onErrorReturn(false);
    }
}
