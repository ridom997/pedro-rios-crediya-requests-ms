package co.com.pedrorido.usecase.apis;

import co.com.pedrorido.model.requestdomain.RequestBasicAdminInfo;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.utils.CreateRequestDomainDTO;
import co.com.pedrorido.model.utils.PageResult;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface IRequestApi {
    Mono<RequestDomain> createRequest(CreateRequestDomainDTO createRequest);
    Mono<RequestDomain> updateStatusRequest(String requestId, Long statusId);
    Mono<PageResult<RequestBasicAdminInfo>> getListByStatus(Set<Long> statusEnumSet, int page, int size);
}
