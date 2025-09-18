package co.com.pedrorido.usecase.request;

import co.com.pedrorido.model.external.RequestStatusChangeMessage;
import co.com.pedrorido.model.external.gateways.MessagePublisherRepository;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.model.utils.StatusEnum;
import co.com.pedrorido.usecase.apis.IRequestApi;
import co.com.pedrorido.usecase.apis.IRequestEventsApi;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RequestEventsUseCase implements IRequestEventsApi {
    private final RequestDomainRepository requestDomainRepository;
    private final MessagePublisherRepository publisherRepository;
    private final IRequestApi requestApi;

    @Override
    public Mono<Void> onStatusChange(RequestStatusChangeMessage incomingData) {
        return requestApi.updateStatusRequest(incomingData.solicitudId(), Long.valueOf(incomingData.estadoNuevo()), false)
                .flatMap(savedRequest -> requestDomainRepository.getRequestFromUserByStatusId(savedRequest.getEmail(), StatusEnum.APPROVED.getId())
                        .flatMap(list -> {
                            Map<UUID, BigDecimal> debtByUser = null;
                            if (savedRequest.getStatusId().equals(StatusEnum.APPROVED.getId())){
                                debtByUser = list.stream()
                                        .filter(request -> request.getId() != null && request.getMonthlyDebt() != null) // Evitar nulos
                                        .collect(Collectors.toMap(RequestDomain::getId, RequestDomain::getMonthlyDebt));
                            }
                            RequestStatusChangeMessage requestStatusChangeMessage = new RequestStatusChangeMessage(
                                    savedRequest.getId(),
                                    StatusEnum.fromId(Long.valueOf(incomingData.estadoAnterior())).getDescription(),
                                    StatusEnum.fromId(savedRequest.getStatusId()).getDescription(),
                                    savedRequest.getEmail(),
                                    new Date(),
                                    debtByUser,
                                    incomingData.reason());
                            return publisherRepository.publishRequestStatusChange(requestStatusChangeMessage);
                        }).then())
                .then();
    }
}
