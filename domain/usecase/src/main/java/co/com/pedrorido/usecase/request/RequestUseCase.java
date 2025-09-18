package co.com.pedrorido.usecase.request;

import co.com.pedrorido.model.external.RequestCalculateDebtMessage;
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
        return loanTypeRepository.findById(createRequest.getTypeLoanId()).switchIfEmpty(Mono.error(new IllegalStateException("loan type not found")))
                .flatMap(loanType -> {
                            if (loanType.getMaximumAmount() != null && createRequest.getAmount().compareTo(loanType.getMaximumAmount()) > 0) {
                                return Mono.error(new IllegalStateException("amount exceeds maximum allowed"));
                            }
                            if (loanType.getMinimumAmount() != null && createRequest.getAmount().compareTo(loanType.getMinimumAmount()) < 0) {
                                return Mono.error(new IllegalStateException("amount below minimum allowed"));
                            }
                            BigDecimal amount = createRequest.getAmount();
                            Integer term = createRequest.getTerm();
                            return userRepository.userExistsByDocumentNumber(createRequest.getDocumentNumber(), createRequest.getEmail())
                                    .flatMap(userExists -> requestDomainRepository.saveRequestDomain(RequestDomain.builder()
                                            .amount(amount)
                                            .typeLoanId(createRequest.getTypeLoanId())
                                            .term(term)
                                            .email(createRequest.getEmail())
                                            .statusId(StatusEnum.PENDING.getId())
                                            .build())).flatMap(res -> {
                                        if (loanType.getAutomaticValidation().equals(Boolean.TRUE)) {
                                            return userRepository.getUserByEmail(createRequest.getEmail())
                                                    .flatMap(user -> {
                                                        if (user == null)
                                                            return Mono.error(new IllegalStateException("user not found"));

                                                        return requestDomainRepository.findSumMonthlyDebtByEmail(createRequest.getEmail()).flatMap(
                                                                sumMonthlyDebt -> {
                                                                    BigDecimal baseSalary = user.getBaseSalary();
                                                                    Double interestRate = loanType.getInterestRate();
                                                                    BigDecimal maximumDebtCapacity = baseSalary.multiply(BigDecimal.valueOf(0.35));
                                                                    BigDecimal currentMonthlyDebt = sumMonthlyDebt;
                                                                    BigDecimal availableClientCapacity = maximumDebtCapacity.subtract(currentMonthlyDebt);
                                                                    BigDecimal newMonthlyDebt = LoanMath.monthlyPayment(amount, BigDecimal.valueOf(interestRate), term, true);
                                                                    RequestCalculateDebtMessage evt = new RequestCalculateDebtMessage(
                                                                            res.getId(), createRequest.getEmail(), null, amount, baseSalary, newMonthlyDebt, availableClientCapacity
                                                                    );
                                                                    return publisherRepository.publishCalculateDebtCapacitySqs(evt).thenReturn(Mono.empty());
                                                                }
                                                        );
                                                    }).then(Mono.just(res));
                                        }
                                        return Mono.just(res);
                                    })
                                    ;

                        }


                );
    }

    @Override
    public Mono<RequestDomain> updateStatusRequest(UUID requestId, Long statusId, boolean callEvent) {
        return validateTargetStatus(statusId)
                .then(requestDomainRepository.findById(requestId)
                        .switchIfEmpty(Mono.error(new IllegalStateException("request not found"))))
                .flatMap(current -> ensurePendingTransition(current)
                        .then(applyStatusChange(current, statusId)))
                .flatMap(updated -> persistWithSideEffects(updated, callEvent));
    }

    // ----------------- helpers -----------------

    /** Valida que el estado objetivo sea APPROVED o REJECTED */
    private Mono<Void> validateTargetStatus(Long statusId) {
        boolean valid = Objects.equals(statusId, StatusEnum.APPROVED.getId())
                || Objects.equals(statusId, StatusEnum.REJECTED.getId()) || Objects.equals(statusId, StatusEnum.REJECTED.getId());
        return valid ? Mono.empty() : Mono.error(new IllegalStateException("invalid status"));
    }

    /** Garantiza que el cambio solo ocurra desde PENDING */
    private Mono<Void> ensurePendingTransition(RequestDomain current) {
        if (!Objects.equals(current.getStatusId(), StatusEnum.PENDING.getId())) {
            return Mono.error(new IllegalStateException("invalid status change"));
        }
        return Mono.empty();
    }

    /** Aplica el cambio de estado en memoria; para APPROVED deja listo el cálculo de monthlyDebt. */
    private Mono<RequestDomain> applyStatusChange(RequestDomain current, Long newStatusId) {
        current.setStatusId(newStatusId);

        if (Objects.equals(newStatusId, StatusEnum.APPROVED.getId())) {
            // calcular monthlyDebt antes de guardar
            return loanTypeRepository.findById(current.getTypeLoanId())
                    .switchIfEmpty(Mono.error(new IllegalStateException("loan type not found")))
                    .map(lt -> {
                        BigDecimal monthly = LoanMath.monthlyPayment(
                                current.getAmount(),
                                BigDecimal.valueOf(lt.getInterestRate()),
                                current.getTerm(),
                                true
                        );
                        current.setMonthlyDebt(monthly);
                        return current;
                    });
        }
        return Mono.just(current);
    }

    /**
     * Persiste y ejecuta efectos colaterales:
     * - APPROVED: solo guardar (mantiene tu comportamiento original).
     * - REJECTED: guardar y publicar evento si callEvent = true.
     */
    private Mono<RequestDomain> persistWithSideEffects(RequestDomain toSave, boolean callEvent) {
        return requestDomainRepository.saveRequestDomain(toSave)
                .flatMap(saved -> {
                    if (!callEvent) {
                        return Mono.just(saved);
                    }

                    RequestStatusChangeMessage evt = buildStatusChangeEvent(saved);
                    return publisherRepository.publishRequestStatusChange(evt)
                            .thenReturn(saved);
                });
    }

    /** Construye el mensaje de evento con los campos requeridos (debtByUser y extra nulos como en tu código). */
    private RequestStatusChangeMessage buildStatusChangeEvent(RequestDomain saved) {
        Long previous = StatusEnum.PENDING.getId();
        Long current  = saved.getStatusId();
        Map<UUID, BigDecimal> debtMap = null;
        if (current.equals(StatusEnum.APPROVED.getId())) {
            debtMap = new HashMap<>();
            debtMap.put(saved.getId(), saved.getMonthlyDebt());
        }
        return new RequestStatusChangeMessage(
                saved.getId(),
                StatusEnum.fromId(previous).getDescription(),
                StatusEnum.fromId(current).getDescription(),
                saved.getEmail(),
                new Date(),
                debtMap,
                null
        );
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
                                if (req.getMonthlyDebt() == null) {
                                    req.setMonthlyDebt(StatusEnum.APPROVED.getId().equals(req.getStatusId()) ?
                                            LoanMath.monthlyPayment(req.getAmount(), BigDecimal.valueOf(req.getInterestRate()), req.getTerm(), true) : null);
                                }
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
