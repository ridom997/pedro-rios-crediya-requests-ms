package co.com.pedrorido.model.external.gateways;

import co.com.pedrorido.model.external.RequestCalculateDebtMessage;
import co.com.pedrorido.model.external.RequestStatusChangeMessage;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface MessagePublisherRepository {
    Mono<Void> publishRequestStatusChange(RequestStatusChangeMessage evt);
    Mono<Void> publishCalculateDebtCapacitySqs(RequestCalculateDebtMessage evt);
    Mono<Void> publishUpdateCounterQueue(Map<String, String> evt);
}
