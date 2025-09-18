package co.com.pedrorido.model.loantype.gateways;

import co.com.pedrorido.model.loantype.LoanType;
import reactor.core.publisher.Mono;

public interface LoanTypeRepository {
    Mono<Boolean> loanTypeExistsById(Long id);
    Mono<LoanType> findById(Long id);
}
