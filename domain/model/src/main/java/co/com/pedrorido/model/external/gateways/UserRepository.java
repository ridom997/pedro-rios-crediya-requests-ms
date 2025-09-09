package co.com.pedrorido.model.external.gateways;

import co.com.pedrorido.model.external.User;
import reactor.core.publisher.Mono;

public interface UserRepository {
    Mono<Boolean> userExistsByDocumentNumber(String documentNumber, String email);
    Mono<User> getUserByEmail(String email);
}
