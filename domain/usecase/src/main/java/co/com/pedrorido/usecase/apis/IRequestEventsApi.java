package co.com.pedrorido.usecase.apis;

import co.com.pedrorido.model.external.RequestStatusChangeMessage;
import reactor.core.publisher.Mono;

public interface IRequestEventsApi {
    Mono<Void> onStatusChange(RequestStatusChangeMessage evt);
}