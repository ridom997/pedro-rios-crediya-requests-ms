package co.com.pedrorido.model.requestdomain.gateways;

import co.com.pedrorido.model.requestdomain.RequestBasicAdminInfo;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.utils.PageResult;
import co.com.pedrorido.model.utils.StatusEnum;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface RequestDomainRepository {
    Mono<RequestDomain> saveRequestDomain(RequestDomain request);
    Mono<PageResult<RequestBasicAdminInfo>> findPage(Set<Long> statusEnumSet, int page, int size);
}
