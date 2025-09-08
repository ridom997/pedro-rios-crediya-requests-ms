package co.com.pedrorido.model.status.gateways;

import co.com.pedrorido.model.status.Status;
import reactor.core.publisher.Mono;

public interface StatusRepository {
    Mono<Status> findById(Long id);
}
