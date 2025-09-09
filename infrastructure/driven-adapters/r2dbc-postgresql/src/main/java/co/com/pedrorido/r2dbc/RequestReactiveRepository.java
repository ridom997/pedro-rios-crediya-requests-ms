package co.com.pedrorido.r2dbc;

import co.com.pedrorido.r2dbc.entity.RequestAndLoanTypeEntity;
import co.com.pedrorido.r2dbc.entity.RequestEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

// TODO: This file is just an example, you should delete or modify it
public interface RequestReactiveRepository extends ReactiveCrudRepository<RequestEntity, String>, ReactiveQueryByExampleExecutor<RequestEntity> {
    @Query("select * from solicitud s where s.id_estado in (:statusNames)")
    Flux<RequestEntity> findByStatusName(List<String> statusNames);
}
