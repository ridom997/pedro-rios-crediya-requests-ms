package co.com.pedrorido.usecase.request;

import co.com.pedrorido.model.external.gateways.UserRepository;
import co.com.pedrorido.model.loantype.gateways.LoanTypeRepository;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.model.utils.CreateRequestDomainDTO;
import co.com.pedrorido.model.utils.StatusEnum;
import co.com.pedrorido.usecase.apis.IRequestApi;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RequestUseCase implements IRequestApi {
    private final LoanTypeRepository loanTypeRepository;
    private final RequestDomainRepository requestDomainRepository;
    private final UserRepository userRepository;

    @Override
    public Mono<RequestDomain> createRequest(CreateRequestDomainDTO createRequest) {
        return loanTypeRepository.loanTypeExistsById(createRequest.getTypeLoanId())
                .flatMap(loanTypeExists -> {
                    if (!loanTypeExists) return Mono.error(new IllegalStateException("loan type not found"));
                    return userRepository.userExistsByDocumentNumber(createRequest.getDocumentNumber());
                }).flatMap(userExists -> {
                    if (!userExists) return Mono.error(new IllegalStateException("user not found"));
                    return requestDomainRepository.saveRequestDomain(RequestDomain.builder()
                            .amount(createRequest.getAmount())
                            .typeLoanId(createRequest.getTypeLoanId())
                            .term(createRequest.getTerm())
                            .email(createRequest.getEmail())
                            .statusId(StatusEnum.PENDING.getDescription())
                            .build());
                });
    }
}
