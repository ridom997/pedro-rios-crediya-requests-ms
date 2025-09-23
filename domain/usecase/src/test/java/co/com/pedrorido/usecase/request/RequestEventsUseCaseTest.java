package co.com.pedrorido.usecase.request;

import co.com.pedrorido.model.external.RequestStatusChangeMessage;
import co.com.pedrorido.model.external.gateways.MessagePublisherRepository;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.model.utils.StatusEnum;
import co.com.pedrorido.usecase.apis.IRequestApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestEventsUseCaseTest {

    @Mock private RequestDomainRepository requestDomainRepository;
    @Mock private MessagePublisherRepository publisherRepository;
    @Mock private IRequestApi requestApi;

    private RequestEventsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RequestEventsUseCase(requestDomainRepository, publisherRepository, requestApi);
    }

    // ---------- helpers reflexión ----------
    private static <T> T set(Object obj, String field, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(obj, value);
            return (T) obj;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private static <T> T newObj(Class<T> c) {
        try { return c.getDeclaredConstructor().newInstance(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // ============= CASO APPROVED =============
    @Test
    void onStatusChange_whenApproved_publishesMailAndCounter_andFiltersDebtMap() {
        // incoming: anterior=PENDING(1), nuevo=APPROVED(2)
        UUID requestId = UUID.randomUUID();
        String email = "user@test.com";
        RequestStatusChangeMessage incoming = new RequestStatusChangeMessage(
                requestId,
                String.valueOf(StatusEnum.PENDING.getId()),
                String.valueOf(StatusEnum.APPROVED.getId()),
                email,
                new Date(),
                null,
                "ok"
        );

        // El request "actualizado" devuelto por IRequestApi
        RequestDomain saved = newObj(RequestDomain.class);
        set(saved, "id", requestId);
        set(saved, "email", email);
        set(saved, "statusId", StatusEnum.APPROVED.getId());
        set(saved, "amount", new BigDecimal("10000.00"));
        set(saved, "monthlyDebt", new BigDecimal("945.60")); // valor cualquiera válido

        when(requestApi.updateStatusRequest(eq(requestId), eq(StatusEnum.APPROVED.getId()), eq(false)))
                .thenReturn(Mono.just(saved));

        // La lista de solicitudes aprobadas del usuario (con elementos "ruidosos" que deben filtrarse)
        RequestDomain validA = newObj(RequestDomain.class);
        set(validA, "id", UUID.randomUUID());
        set(validA, "monthlyDebt", new BigDecimal("111.11"));

        RequestDomain noId = newObj(RequestDomain.class);
        set(noId, "id", null);
        set(noId, "monthlyDebt", new BigDecimal("222.22"));

        RequestDomain noDebt = newObj(RequestDomain.class);
        set(noDebt, "id", UUID.randomUUID());
        set(noDebt, "monthlyDebt", null);

        when(requestDomainRepository.getRequestFromUserByStatusId(email, StatusEnum.APPROVED.getId()))
                .thenReturn(Mono.just(List.of(validA, noId, noDebt)));

        when(publisherRepository.publishRequestStatusChange(any())).thenReturn(Mono.empty());
        when(publisherRepository.publishUpdateCounterQueue(anyMap())).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(useCase.onStatusChange(incoming)).verifyComplete();

        // Assert: publicaron el correo/evento con debtMap filtrado
        ArgumentCaptor<RequestStatusChangeMessage> evtCap = ArgumentCaptor.forClass(RequestStatusChangeMessage.class);
        verify(publisherRepository, times(1)).publishRequestStatusChange(evtCap.capture());
        RequestStatusChangeMessage sent = evtCap.getValue();

        // estadoAnterior/estadoNuevo en descripciones de StatusEnum
        assertEquals(StatusEnum.fromId(StatusEnum.PENDING.getId()).getDescription(), sent.estadoAnterior());
        assertEquals(StatusEnum.fromId(StatusEnum.APPROVED.getId()).getDescription(), sent.estadoNuevo());
        assertEquals(email, sent.usuarioId());
        assertEquals(requestId, sent.solicitudId());
        assertEquals("ok", sent.reason());

        // debtMap debe contener SOLO el válido
        assertNotNull(sent.debtMap());
        assertEquals(1, sent.debtMap().size());
        assertEquals(new BigDecimal("111.11"), sent.debtMap().get(validA.getId()));

        // counter publicado con pk=approvedLoans y totalAmountLoans=amount
        ArgumentCaptor<Map<String, String>> mapCap = ArgumentCaptor.forClass(Map.class);
        verify(publisherRepository, times(1)).publishUpdateCounterQueue(mapCap.capture());
        Map<String, String> payload = mapCap.getValue();
        assertEquals("approvedLoans", payload.get("pk"));
        assertEquals("10000.00", payload.get("totalAmountLoans"));

        // siempre se consulta el repo por aprobados del usuario
        verify(requestDomainRepository, times(1))
                .getRequestFromUserByStatusId(email, StatusEnum.APPROVED.getId());
    }

    // ============= CASO REJECTED =============
    @Test
    void onStatusChange_whenRejected_publishesMail_only_noCounter() {
        UUID requestId = UUID.randomUUID();
        String email = "user@test.com";
        RequestStatusChangeMessage incoming = new RequestStatusChangeMessage(
                requestId,
                String.valueOf(StatusEnum.PENDING.getId()),
                String.valueOf(StatusEnum.REJECTED.getId()),
                email,
                new Date(),
                null,
                "nope"
        );

        RequestDomain saved = newObj(RequestDomain.class);
        set(saved, "id", requestId);
        set(saved, "email", email);
        set(saved, "statusId", StatusEnum.REJECTED.getId());
        set(saved, "amount", new BigDecimal("7777.77"));

        when(requestApi.updateStatusRequest(eq(requestId), eq(StatusEnum.REJECTED.getId()), eq(false)))
                .thenReturn(Mono.just(saved));

        // Igual se consulta la lista de aprobados del usuario (aunque esté REJECTED)
        when(requestDomainRepository.getRequestFromUserByStatusId(email, StatusEnum.APPROVED.getId()))
                .thenReturn(Mono.just(List.of())); // vacío

        when(publisherRepository.publishRequestStatusChange(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.onStatusChange(incoming)).verifyComplete();

        // Se publica el evento
        verify(publisherRepository, times(1)).publishRequestStatusChange(any());

        // No se publica counter
        verify(publisherRepository, never()).publishUpdateCounterQueue(anyMap());

        // Consulta a aprobados igual se realiza
        verify(requestDomainRepository, times(1))
                .getRequestFromUserByStatusId(email, StatusEnum.APPROVED.getId());
    }

    // ============= ERROR TEMPRANO =============
    @Test
    void onStatusChange_whenUpdateStatusFails_propagatesError_andSkipsSideEffects() {
        UUID requestId = UUID.randomUUID();
        String email = "user@test.com";
        RequestStatusChangeMessage incoming = new RequestStatusChangeMessage(
                requestId,
                String.valueOf(StatusEnum.PENDING.getId()),
                String.valueOf(StatusEnum.APPROVED.getId()),
                email,
                new Date(),
                null,
                "err"
        );

        when(requestApi.updateStatusRequest(eq(requestId), eq(StatusEnum.APPROVED.getId()), eq(false)))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(useCase.onStatusChange(incoming))
                .expectErrorMatches(ex -> ex instanceof RuntimeException && ex.getMessage().equals("boom"))
                .verify();

        // No se toca el resto
        verifyNoInteractions(requestDomainRepository);
        verifyNoInteractions(publisherRepository);
    }
}