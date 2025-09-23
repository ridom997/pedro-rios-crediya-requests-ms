package co.com.pedrorido.usecase.request;

import co.com.pedrorido.model.external.RequestCalculateDebtMessage;
import co.com.pedrorido.model.external.User;
import co.com.pedrorido.model.external.gateways.MessagePublisherRepository;
import co.com.pedrorido.model.external.gateways.UserRepository;
import co.com.pedrorido.model.loantype.LoanType;
import co.com.pedrorido.model.loantype.gateways.LoanTypeRepository;
import co.com.pedrorido.model.requestdomain.RequestBasicAdminInfo;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.model.utils.CreateRequestDomainDTO;
import co.com.pedrorido.model.utils.PageResult;
import co.com.pedrorido.model.utils.StatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests 100% coverage para RequestUseCase.
 * Requiere que RequestUseCase tenga un constructor con los 4 repos.
 */
@ExtendWith(MockitoExtension.class)
class RequestUseCaseTest {

    // ====== Mocks de repos ======
    @Mock
    LoanTypeRepository loanTypeRepository;
    @Mock
    RequestDomainRepository requestDomainRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    MessagePublisherRepository publisherRepository;

    // ====== SUT ======
    private RequestUseCase useCase;

    @BeforeEach
    void init() {
        useCase = new RequestUseCase(loanTypeRepository, requestDomainRepository, userRepository, publisherRepository);
    }

    // =========================
    // createRequest – errores
    // =========================

    @Test
    void createRequest_loanTypeNotFound_errors() {
        CreateRequestDomainDTO dto = mockCreateRequestDomainDTO();
        when(loanTypeRepository.findById(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.createRequest(dto))
                .expectErrorSatisfies(ex -> assertTrue(ex instanceof IllegalStateException
                        && ex.getMessage().contains("loan type not found")))
                .verify();
    }

    @Test
    void createRequest_amountExceedsMax_errors() {
        CreateRequestDomainDTO dto = mockCreateRequestDomainDTO();
        dto.setAmount(new BigDecimal("50001.00"));
        LoanType lt = mockLoanType();

        when(loanTypeRepository.findById(any())).thenReturn(Mono.just(lt));

        StepVerifier.create(useCase.createRequest(dto))
                .expectErrorSatisfies(ex -> assertTrue(ex instanceof IllegalStateException
                        && ex.getMessage().contains("amount exceeds maximum allowed")))
                .verify();
    }

    @Test
    void createRequest_amountBelowMin_errors() {
        CreateRequestDomainDTO dto = mockCreateRequestDomainDTO();
        dto.setAmount(new BigDecimal("1.00"));
        LoanType lt = mockLoanType();

        when(loanTypeRepository.findById(any())).thenReturn(Mono.just(lt));

        StepVerifier.create(useCase.createRequest(dto))
                .expectErrorSatisfies(ex -> assertTrue(ex instanceof IllegalStateException
                        && ex.getMessage().contains("amount below minimum allowed")))
                .verify();
    }

    // =========================
    // createRequest – happy paths
    // =========================

    CreateRequestDomainDTO mockCreateRequestDomainDTO(){
        CreateRequestDomainDTO createRequestDomainDTO = new CreateRequestDomainDTO();
        createRequestDomainDTO.setDocumentNumber("123456789");
        createRequestDomainDTO.setEmail("example@example.com");
        createRequestDomainDTO.setAmount(new BigDecimal("1000.00"));
        createRequestDomainDTO.setTerm(12);
        createRequestDomainDTO.setTypeLoanId(1L);
        return createRequestDomainDTO;
    }

    LoanType mockLoanType(){
        LoanType loanType = new LoanType();
        loanType.setId(1L);
        loanType.setName("Personal Loan");
        loanType.setMinimumAmount(new BigDecimal("500.00"));
        loanType.setMaximumAmount(new BigDecimal("50000.00"));
        loanType.setInterestRate(3.5);
        loanType.setAutomaticValidation(true);
        return loanType;
    }

    RequestDomain mockRequestDomain(){
        RequestDomain requestDomain = new RequestDomain();
        requestDomain.setId(UUID.randomUUID());
        requestDomain.setAmount(new BigDecimal("1000.00"));
        requestDomain.setTerm(12);
        requestDomain.setEmail("example@example.com");
        requestDomain.setStatusId(1L);
        requestDomain.setTypeLoanId(2L);
        requestDomain.setMonthlyDebt(new BigDecimal("120.00"));
        return requestDomain;
    }

    User mockUser(){
        User user = new User();
        user.setName("John");
        user.setSurname("Doe");
        user.setBirthDate(LocalDate.of(1990, 1, 15));
        user.setAddress("123 Main Street, City, Country");
        user.setPhone("123-456-7890");
        user.setEmail("johndoe@example.com");
        user.setBaseSalary(new BigDecimal("3000.00"));
        user.setRoleId(1L); // Identificador del rol, por ejemplo, 1 para "Administrador"
        user.setDocumentNumber("ABC123456");
        return user;
    }

    RequestBasicAdminInfo mockRequestBasicAdminInfo(){
        RequestBasicAdminInfo requestInfo = new RequestBasicAdminInfo();
        requestInfo.setId(UUID.randomUUID());
        requestInfo.setAmount(new BigDecimal("1500.00"));
        requestInfo.setTerm(24);
        requestInfo.setEmail("client@example.com");
        requestInfo.setClientName("Jane Doe");
        requestInfo.setTypeLoanId(3L); // Por ejemplo, 3 para "Préstamo Hipotecario"
        requestInfo.setInterestRate(4.5);
        requestInfo.setBaseSalary(new BigDecimal("2500.00"));
        requestInfo.setStatusId(2L); // Estado: 2 podría representar "Aprobado"
        requestInfo.setMonthlyDebt(new BigDecimal("125.00"));
        return requestInfo;
    }
    @Test
    void createRequest_automaticValidationFalse_savesAndReturns() {
        CreateRequestDomainDTO dto = mockCreateRequestDomainDTO();

        LoanType lt = mockLoanType();

        RequestDomain saved = mockRequestDomain();
        User user = mockUser();
        when(loanTypeRepository.findById(any())).thenReturn(Mono.just(lt));
        when(userRepository.userExistsByDocumentNumber(any(), any())).thenReturn(Mono.just(true));
        when(requestDomainRepository.saveRequestDomain(any(RequestDomain.class))).thenReturn(Mono.just(saved));
        when(userRepository.getUserByEmail(any())).thenReturn(Mono.just(user));
        when(requestDomainRepository.findSumMonthlyDebtByEmail(any())).thenReturn(Mono.just(new BigDecimal("1000")));
        when(publisherRepository.publishCalculateDebtCapacitySqs(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.createRequest(dto))
                .expectNextMatches(res -> Objects.equals(get(res, "id"), get(saved, "id")))
                .verifyComplete();

    }

    @Test
    void createRequest_automaticValidationTrue_calculatesAndPublishes_thenReturns() {
        CreateRequestDomainDTO dto = mockCreateRequestDomainDTO();

        LoanType lt = mockLoanType();

        RequestDomain saved = mockRequestDomain();
        User user = mockUser();

        when(loanTypeRepository.findById(any())).thenReturn(Mono.just(lt));
        when(userRepository.userExistsByDocumentNumber(any(), any())).thenReturn(Mono.just(true));
        when(requestDomainRepository.saveRequestDomain(any(RequestDomain.class))).thenReturn(Mono.just(saved));
        when(userRepository.getUserByEmail(any())).thenReturn(Mono.just(user));
        when(requestDomainRepository.findSumMonthlyDebtByEmail(any())).thenReturn(Mono.just(new BigDecimal("1000")));
        when(publisherRepository.publishCalculateDebtCapacitySqs(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.createRequest(dto))
                .expectNextMatches(res -> Objects.equals(get(res, "id"), get(saved, "id")))
                .verifyComplete();

        // Capturamos el mensaje publicado para validar cálculos (newMonthlyDebt y availableClientCapacity)
        ArgumentCaptor<RequestCalculateDebtMessage> cap = ArgumentCaptor.forClass(RequestCalculateDebtMessage.class);
        verify(publisherRepository).publishCalculateDebtCapacitySqs(cap.capture());
        RequestCalculateDebtMessage msg = cap.getValue();

        assertEquals(get(saved, "id"), msg.transactionId());
        assertEquals("example@example.com", msg.email());
        assertEquals(new BigDecimal("1000.00"), msg.loanAmount());
        assertEquals(new BigDecimal("3000.00"), msg.baseSalary());

        // availableClientCapacity = baseSalary * 0.35 - currentMonthlyDebt = 1750 - 1000 = 750
        assertEquals(new BigDecimal("50.00"), msg.availableClientCapacity().setScale(2));

        // cuota esperada con 0.02 mensual a 12 (mismo LoanMath que arriba): 9455.96 para 100000;
        // aquí es 10000 -> 945.60 (aprox). Verificamos 2 decimales HALF_EVEN.
        assertEquals(new BigDecimal("3500.00"), msg.newMonthlyDebt());
    }

    @Test
    void updateStatus_requestNotFound_errors() {
        UUID id = UUID.randomUUID();
        when(requestDomainRepository.findById(id)).thenReturn(Mono.empty());
        StepVerifier.create(useCase.updateStatusRequest(id, StatusEnum.APPROVED.getId(), false))
                .expectErrorSatisfies(ex -> assertTrue(ex instanceof IllegalStateException
                        && ex.getMessage().contains("request not found")))
                .verify();
    }

    @Test
    void updateStatus_invalidTransition_notFromPending_errors() {
        UUID id = UUID.randomUUID();
        RequestDomain current = mockRequestDomain(); // no viene de PENDING
        current.setStatusId(2L);
        when(requestDomainRepository.findById(id)).thenReturn(Mono.just(current));

        StepVerifier.create(useCase.updateStatusRequest(id, StatusEnum.REJECTED.getId(), false))
                .expectErrorSatisfies(ex -> assertTrue(ex instanceof IllegalStateException
                        && ex.getMessage().contains("invalid status change")))
                .verify();
    }

    // =========================
    // updateStatusRequest – APPROVED
    // =========================

    @Test
    void updateStatus_approved_setsMonthlyDebt_saves_andPublishesCounterWhenCallEventTrue() {
        UUID id = UUID.randomUUID();
        RequestDomain current = mockRequestDomain();

        LoanType lt = mockLoanType();

        RequestDomain saved = mockRequestDomain(); // esperado por LoanMath

        when(requestDomainRepository.findById(id)).thenReturn(Mono.just(current));
        when(loanTypeRepository.findById(any())).thenReturn(Mono.just(lt));
        when(requestDomainRepository.saveRequestDomain(any(RequestDomain.class)))
                .thenAnswer(inv -> Mono.just((RequestDomain) inv.getArgument(0)));
        when(publisherRepository.publishRequestStatusChange(any())).thenReturn(Mono.empty());
        when(publisherRepository.publishUpdateCounterQueue(anyMap())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatusRequest(id, StatusEnum.APPROVED.getId(), true))
                .expectNextMatches(out -> {
                    assertEquals(StatusEnum.APPROVED.getId(), get(out, "statusId"));
                    assertEquals(new BigDecimal("3500.00"), ((RequestDomain) out).getMonthlyDebt());
                    return true;
                })
                .verifyComplete();

        // Publish de estado y luego counter
        verify(publisherRepository).publishRequestStatusChange(any());
        ArgumentCaptor<Map<String,String>> cap = ArgumentCaptor.forClass(Map.class);
        verify(publisherRepository).publishUpdateCounterQueue(cap.capture());
        Map<String,String> payload = cap.getValue();
        assertEquals("approvedLoans", payload.get("pk"));
        assertEquals("1000.00", payload.get("totalAmountLoans"));
    }

    @Test
    void updateStatus_approved_withoutCallEvent_doesNotPublishAnything() {
        UUID id = UUID.randomUUID();
        RequestDomain current = mockRequestDomain();

        LoanType lt = mockLoanType();

        when(requestDomainRepository.findById(id)).thenReturn(Mono.just(current));
        when(loanTypeRepository.findById(any())).thenReturn(Mono.just(lt));
        when(requestDomainRepository.saveRequestDomain(any(RequestDomain.class)))
                .thenAnswer(inv -> Mono.just((RequestDomain) inv.getArgument(0)));

        StepVerifier.create(useCase.updateStatusRequest(id, StatusEnum.APPROVED.getId(), false))
                .expectNextCount(1)
                .verifyComplete();

        verify(publisherRepository, never()).publishRequestStatusChange(any());
        verify(publisherRepository, never()).publishUpdateCounterQueue(anyMap());
    }

    // =========================
    // updateStatusRequest – REJECTED
    // =========================

    @Test
    void updateStatus_rejected_saves_andPublishesEventOnlyWhenCallEventTrue() {
        UUID id = UUID.randomUUID();
        RequestDomain current = mockRequestDomain();

        when(requestDomainRepository.findById(id)).thenReturn(Mono.just(current));
        when(requestDomainRepository.saveRequestDomain(any(RequestDomain.class)))
                .thenAnswer(inv -> Mono.just((RequestDomain) inv.getArgument(0)));
        when(publisherRepository.publishRequestStatusChange(any())).thenReturn(Mono.empty());

        // callEvent = true -> publica solo evento de estado (no counter)
        StepVerifier.create(useCase.updateStatusRequest(id, StatusEnum.REJECTED.getId(), true))
                .expectNextMatches(out -> Objects.equals(get(out, "statusId"), StatusEnum.REJECTED.getId()))
                .verifyComplete();
        verify(publisherRepository).publishRequestStatusChange(any());
        verify(publisherRepository, never()).publishUpdateCounterQueue(anyMap());
    }

    // =========================
    // getListByStatus – enriquecimiento
    // =========================

    @Test
    void getListByStatus_enrichesClientNameSalary_andComputesMonthlyDebtForApproved() {
        // Datos de page
        RequestBasicAdminInfo r1 = mockRequestBasicAdminInfo(); // debe calcularse

        RequestBasicAdminInfo r2 = mockRequestBasicAdminInfo(); // ya viene

        PageResult<RequestBasicAdminInfo> page = new PageResult<>(List.of(r1, r2), 0, 2, 2, 1);

        Mockito.<Mono<PageResult<RequestBasicAdminInfo>>>when(
                requestDomainRepository.findPage(anySet(), eq(0), eq(2))
        ).thenReturn(Mono.just(page));

        // Usuarios: el primero existe, el segundo falla (onErrorResume lo ignora)
        User u1 = mockUser();

        when(userRepository.getUserByEmail(any())).thenReturn(Mono.just(u1));
        when(userRepository.getUserByEmail(any())).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(useCase.getListByStatus(Set.of(StatusEnum.PENDING.getId(), StatusEnum.APPROVED.getId()), 0, 2))
                .assertNext(out -> {
                    List<RequestBasicAdminInfo> content = out.content();
                    assertEquals(2, content.size());

                    RequestBasicAdminInfo e1 = content.get(0);
                    assertEquals("Jane Doe", e1.getClientName());

                    RequestBasicAdminInfo e2 = content.get(1);
                    // monthlyDebt se mantiene
                    assertEquals(new BigDecimal("125.00"), e2.getMonthlyDebt());

                    assertEquals(0, out.page());
                    assertEquals(2, out.size());
                    assertEquals(2, out.totalElements());
                    assertEquals(1, out.totalPages());
                })
                .verifyComplete();
    }

    // ====== helpers de lectura por reflexión ======
    static Object get(Object obj, String field) {
        try {
            Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
