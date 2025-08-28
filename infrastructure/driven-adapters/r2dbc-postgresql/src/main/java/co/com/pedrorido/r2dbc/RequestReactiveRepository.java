package co.com.pedrorido.r2dbc;

import co.com.pedrorido.r2dbc.entity.RequestEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

// TODO: This file is just an example, you should delete or modify it
public interface RequestReactiveRepository extends ReactiveCrudRepository<RequestEntity, String>, ReactiveQueryByExampleExecutor<RequestEntity> {

}
