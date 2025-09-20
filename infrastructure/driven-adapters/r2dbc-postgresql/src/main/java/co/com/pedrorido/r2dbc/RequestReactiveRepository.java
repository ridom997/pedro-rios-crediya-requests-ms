package co.com.pedrorido.r2dbc;

import co.com.pedrorido.r2dbc.entity.RequestEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// TODO: This file is just an example, you should delete or modify it
public interface RequestReactiveRepository extends ReactiveCrudRepository<RequestEntity, UUID>, ReactiveQueryByExampleExecutor<RequestEntity> {
    @Query("select * from solicitud s where s.id_estado in (:statusNames)")
    Flux<RequestEntity> findByStatusName(List<String> statusNames);

    @Query("""
    SELECT COALESCE(SUM(deuda_mensual), 0)
    FROM solicitud
    WHERE email = :email AND id_estado = '2'
    """)
    Mono<BigDecimal> sumMonthlyDebtByEmail(String email);

    @Query("""
        SELECT id_solicitud, monto, plazo, email, id_estado, id_tipo_prestamo, deuda_mensual
        FROM solicitud
        WHERE LOWER(email) = LOWER(:email) AND id_estado = :statusId
        ORDER BY id_solicitud
    """)
    Flux<RequestEntity> findAllByEmailAndStatusId(String email, Long statusId);
}
