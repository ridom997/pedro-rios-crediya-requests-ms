package co.com.pedrorido.r2dbc;

import co.com.pedrorido.model.loantype.LoanType;
import co.com.pedrorido.model.loantype.gateways.LoanTypeRepository;
import co.com.pedrorido.r2dbc.entity.LoanTypeEntity;
import co.com.pedrorido.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.log4j.Log4j2;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Log4j2
@Repository
public class LoanTypeReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        LoanType,
        LoanTypeEntity,
        Long,
        LoanTypeReactiveRepository
        > implements LoanTypeRepository {
    public LoanTypeReactiveRepositoryAdapter(LoanTypeReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, LoanType.class));
    }

    @Override
    public Mono<Boolean> loanTypeExistsById(Long id) {
        log.info("loanTypeExistsById: {}", id);
        return repository.existsById(id);
    }
}
