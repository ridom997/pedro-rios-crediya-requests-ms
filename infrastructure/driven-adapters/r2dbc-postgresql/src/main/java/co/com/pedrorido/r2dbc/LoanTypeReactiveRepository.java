package co.com.pedrorido.r2dbc;

import co.com.pedrorido.r2dbc.entity.LoanTypeEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

// TODO: This file is just an example, you should delete or modify it
public interface LoanTypeReactiveRepository extends ReactiveCrudRepository<LoanTypeEntity, Long>, ReactiveQueryByExampleExecutor<LoanTypeEntity> {
}
