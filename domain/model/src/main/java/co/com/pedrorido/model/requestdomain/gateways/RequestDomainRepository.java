package co.com.pedrorido.model.requestdomain.gateways;

import co.com.pedrorido.model.requestdomain.RequestDomain;
import reactor.core.publisher.Mono;

public interface RequestDomainRepository {
    Mono<RequestDomain> saveRequestDomain(RequestDomain request);
}
