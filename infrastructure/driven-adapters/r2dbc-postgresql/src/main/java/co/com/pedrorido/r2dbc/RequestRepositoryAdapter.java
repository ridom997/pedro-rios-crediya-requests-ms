package co.com.pedrorido.r2dbc;

import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.r2dbc.entity.RequestEntity;
import co.com.pedrorido.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

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
        return super.save(request);
    }
}
