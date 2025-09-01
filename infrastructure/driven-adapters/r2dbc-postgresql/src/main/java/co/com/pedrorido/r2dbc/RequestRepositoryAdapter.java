package co.com.pedrorido.r2dbc;

import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.r2dbc.entity.RequestEntity;
import co.com.pedrorido.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.log4j.Log4j2;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Log4j2
@Repository
public class RequestRepositoryAdapter extends ReactiveAdapterOperations<
        RequestDomain,
        RequestEntity,
        String,
        RequestReactiveRepository
        > implements RequestDomainRepository {
    public RequestRepositoryAdapter(RequestReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, RequestDomain.class));
    }

    @Override
    public Mono<RequestDomain> saveRequestDomain(RequestDomain request) {
        log.info("POSTGRES - saveRequestDomain: {}", request);
        return super.save(request);
    }
}
