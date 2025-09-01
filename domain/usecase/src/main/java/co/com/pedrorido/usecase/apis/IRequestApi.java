package co.com.pedrorido.usecase.apis;

import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.utils.CreateRequestDomainDTO;
import reactor.core.publisher.Mono;

public interface IRequestApi {
    Mono<RequestDomain> createRequest(CreateRequestDomainDTO createRequest);
}
