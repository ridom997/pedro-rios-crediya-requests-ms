package co.com.pedrorido.model.loantype.gateways;

import reactor.core.publisher.Mono;

public interface LoanTypeRepository {
    Mono<Boolean> loanTypeExistsById(Long id);
}
