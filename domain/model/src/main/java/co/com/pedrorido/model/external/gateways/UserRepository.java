package co.com.pedrorido.model.external.gateways;

import reactor.core.publisher.Mono;

public interface UserRepository {
    Mono<Boolean> userExistsByDocumentNumber(String documentNumber);
}
