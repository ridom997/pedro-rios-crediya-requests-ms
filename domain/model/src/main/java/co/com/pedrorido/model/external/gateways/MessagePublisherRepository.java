package co.com.pedrorido.model.external.gateways;

import co.com.pedrorido.model.external.RequestStatusChangeMessage;
import reactor.core.publisher.Mono;

public interface MessagePublisherRepository {
    Mono<Void> publishRequestStatusChange(RequestStatusChangeMessage evt);
}
