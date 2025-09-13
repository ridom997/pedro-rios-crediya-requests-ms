package co.com.pedrorido.r2dbc;

import co.com.pedrorido.model.requestdomain.RequestBasicAdminInfo;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.model.utils.PageResult;
import co.com.pedrorido.r2dbc.entity.LoanTypeEntity;
import co.com.pedrorido.r2dbc.entity.RequestEntity;
import co.com.pedrorido.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.log4j.Log4j2;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Repository
public class RequestRepositoryAdapter extends ReactiveAdapterOperations<
        RequestDomain,
        RequestEntity,
        String,
        RequestReactiveRepository
        > implements RequestDomainRepository {
    public RequestRepositoryAdapter(RequestReactiveRepository repository, ObjectMapper mapper, R2dbcEntityTemplate template) {
        super(repository, mapper, d -> mapper.map(d, RequestDomain.class));
        this.template = template;
    }

    private final R2dbcEntityTemplate template;

    @Override
    public Mono<RequestDomain> saveRequestDomain(RequestDomain request) {
        log.info("POSTGRES - saveRequestDomain: {}", request);
        return super.save(request);
    }

    @Override
    public Mono<PageResult<RequestBasicAdminInfo>> findPage(Set<Long> statusEnumSet, int page, int size) {
        log.info("REQUEST-ADAPTER findPage: status={}, page={}, size={}", statusEnumSet, page, size);

        final int p = Math.max(0, page);
        final int s = Math.max(1, size);

        // 1) Filtro base por estados
        final List<Long> statusIds = (statusEnumSet == null || statusEnumSet.isEmpty())
                ? List.of()
                : statusEnumSet.stream().toList();

        final Query base = statusIds.isEmpty()
                ? Query.empty()
                : Query.query(Criteria.where("id_estado").in(statusIds));

        final Query pageQ = base.limit(s).offset((long) p * s);

        // 2) Traer página de solicitudes
        Mono<List<RequestEntity>> requestsMono = template.select(RequestEntity.class)
                .matching(pageQ)
                .all()
                .collectList();

        // 3) Conteo total (con el mismo filtro)
        Mono<Long> totalMono = template.count(base, RequestEntity.class);

        // 4) Componer Read Model
        return Mono.zip(requestsMono, totalMono)
                .flatMap(tuple -> {
                    List<RequestEntity> requests = tuple.getT1();
                    long totalElements = tuple.getT2();

                    // 4.a) IDs únicos de tipo de préstamo en la página
                    Set<Long> typeLoanIds = requests.stream()
                            .map(RequestEntity::getTypeLoanId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                    // 4.b) Traer tipos de préstamo solo si hay ids
                    Mono<Map<Long, LoanTypeEntity>> loanTypesMono = typeLoanIds.isEmpty()
                            ? Mono.just(Collections.emptyMap())
                            : template.select(LoanTypeEntity.class)
                            .matching(Query.query(Criteria.where("id_tipo_prestamo").in(typeLoanIds)))
                            .all()
                            .collectMap(LoanTypeEntity::getId, Function.identity());

                    // 4.c) Enriquecer DTOs
                    return loanTypesMono.map(loanTypes -> {
                        List<RequestBasicAdminInfo> content = requests.stream()
                                .map(r -> {
                                    LoanTypeEntity lt = loanTypes.get(r.getTypeLoanId());
                                    return RequestBasicAdminInfo.builder()
                                            .id(r.getId() != null ? r.getId().toString() : null)
                                            .amount(r.getAmount())
                                            .term(r.getTerm())
                                            .email(r.getEmail())
                                            .statusId(r.getStatusId())
                                            .typeLoanId(r.getTypeLoanId())
                                            .interestRate(lt != null ? lt.getInterestRate() : null)
                                            // clientName, baseSalary, calculated se completan en otra capa si aplica
                                            .build();
                                })
                                .toList();

                        int totalPages = (int) Math.ceil(totalElements / (double) s);
                        return new PageResult<>(content, p, s, totalElements, totalPages);
                    });
                });
    }
}
