package co.com.pedrorido.model.requestdomain.gateways;

import co.com.pedrorido.model.requestdomain.RequestBasicAdminInfo;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.utils.PageResult;
import co.com.pedrorido.model.utils.StatusEnum;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface RequestDomainRepository {
    Mono<RequestDomain> saveRequestDomain(RequestDomain request);
    Mono<RequestDomain> findById(UUID requestId);
    Mono<BigDecimal> findSumMonthlyDebtByEmail(String email);
    Mono<PageResult<RequestBasicAdminInfo>> findPage(Set<Long> statusEnumSet, int page, int size);
    Mono<List<RequestDomain>> getRequestFromUserByStatusId(String userId, Long statusId);
}
