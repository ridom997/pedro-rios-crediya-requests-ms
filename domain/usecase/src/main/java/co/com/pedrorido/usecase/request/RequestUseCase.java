package co.com.pedrorido.usecase.request;

import co.com.pedrorido.model.external.RequestStatusChangeMessage;
import co.com.pedrorido.model.external.User;
import co.com.pedrorido.model.external.gateways.MessagePublisherRepository;
import co.com.pedrorido.model.external.gateways.UserRepository;
import co.com.pedrorido.model.loantype.gateways.LoanTypeRepository;
import co.com.pedrorido.model.requestdomain.RequestBasicAdminInfo;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.model.utils.CreateRequestDomainDTO;
import co.com.pedrorido.model.utils.PageResult;
import co.com.pedrorido.model.utils.StatusEnum;
import co.com.pedrorido.usecase.apis.IRequestApi;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class RequestUseCase implements IRequestApi {
    private final LoanTypeRepository loanTypeRepository;
    private final RequestDomainRepository requestDomainRepository;
    private final UserRepository userRepository;
    private final MessagePublisherRepository publisherRepository;

    @Override
    public Mono<RequestDomain> createRequest(CreateRequestDomainDTO createRequest) {
        return loanTypeRepository.loanTypeExistsById(createRequest.getTypeLoanId())
                .flatMap(loanTypeExists -> {
                    if (!loanTypeExists) return Mono.error(new IllegalStateException("loan type not found"));
                    return userRepository.userExistsByDocumentNumber(createRequest.getDocumentNumber(), createRequest.getEmail());
                }).flatMap(userExists -> {
                    if (!userExists) return Mono.error(new IllegalStateException("user not found"));
                    return requestDomainRepository.saveRequestDomain(RequestDomain.builder()
                            .amount(createRequest.getAmount())
                            .typeLoanId(createRequest.getTypeLoanId())
                            .term(createRequest.getTerm())
                            .email(createRequest.getEmail())
                            .statusId(StatusEnum.PENDING.getId())
                            .build());
                });
    }

    @Override
    public Mono<RequestDomain> updateStatusRequest(String requestId, Long statusId) {
        if (statusId.equals(StatusEnum.APPROVED.getId()) && (statusId.equals(StatusEnum.APPROVED.getId()) || statusId.equals(StatusEnum.REJECTED.getId()))) {
            return requestDomainRepository.findById(requestId)
                    .switchIfEmpty(Mono.error(new IllegalStateException("request not found")))
                    .flatMap(requestDomain -> {
                        if (!requestDomain.getStatusId().equals(StatusEnum.PENDING.getId())) {
                            return Mono.error(new IllegalStateException("invalid status change"));
                        }
                        requestDomain.setStatusId(statusId);
                        return requestDomainRepository.saveRequestDomain(requestDomain);
                    }).doOnSuccess(requestDomain -> {
                        var evt = new RequestStatusChangeMessage(
                                requestDomain.getId(), StatusEnum.PENDING.getDescription(), StatusEnum.fromId(statusId).getDescription(), requestDomain.getEmail(), Instant.now());
                        publisherRepository.publishRequestStatusChange(evt);
                    });
        }
        return Mono.error(new IllegalStateException("invalid status"));
    }

    @Override
    public Mono<PageResult<RequestBasicAdminInfo>> getListByStatus(Set<Long> statusEnumSet, int page, int size) {
        return requestDomainRepository.findPage(statusEnumSet, page, size)
                .flatMap(pageResult -> {
                    // 1) Extrae la lista paginada
                    List<RequestBasicAdminInfo> content = pageResult.content();

                    // 2) Emails únicos (preserva orden de aparición)
                    LinkedHashSet<String> uniqueEmails = content.stream()
                            .map(RequestBasicAdminInfo::getEmail)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                    // 3) Trae los usuarios una sola vez por email (en paralelo), ignora fallos puntuales
                    Mono<Map<String, User>> usersByEmailMono = Flux.fromIterable(uniqueEmails)
                            .flatMap(email ->
                                            userRepository.getUserByEmail(email)
                                                    .map(user -> Map.entry(email, user))
                                                    .onErrorResume(e -> Mono.empty()),
                                    8 // concurrency sugerida
                            )
                            .collectMap(Map.Entry::getKey, Map.Entry::getValue);

                    // 4) Rellena nameUser en todos los items y retorna el mismo pageResult con content enriquecido
                    return usersByEmailMono.map(usersByEmail -> {
                        content.forEach(req -> {
                            User u = usersByEmail.get(req.getEmail());
                            if (u != null) {
                                String fullName = Stream.of(u.getName(), u.getSurname())
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.joining(" "));
                                req.setClientName(fullName);
                                req.setBaseSalary(u.getBaseSalary());
                                req.setMonthlyDebt(StatusEnum.APPROVED.getId().equals(req.getStatusId()) ?
                                        LoanMath.monthlyPayment(req.getAmount(), BigDecimal.valueOf(req.getInterestRate()), req.getTerm(), true) : null);
                            }
                        });

                        return new PageResult<>(
                                content,
                                pageResult.page(),
                                pageResult.size(),
                                pageResult.totalElements(),
                                pageResult.totalPages()
                        );
                    });
                });
    }
}
