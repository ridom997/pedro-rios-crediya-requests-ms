package co.com.pedrorido.usecase.request;

import co.com.pedrorido.model.external.User;
import co.com.pedrorido.model.external.gateways.UserRepository;
import co.com.pedrorido.model.loantype.gateways.LoanTypeRepository;
import co.com.pedrorido.model.requestdomain.RequestBasicAdminInfo;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.model.utils.CreateRequestDomainDTO;
import co.com.pedrorido.model.utils.PageResult;
import co.com.pedrorido.model.utils.StatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestUseCaseTest {
    private LoanTypeRepository loanTypeRepository;
    private RequestDomainRepository requestDomainRepository;
    private RequestUseCase requestUseCase;
    private UserRepository userRepository;
    private static final Long APPROVED_ID = StatusEnum.APPROVED.getId();

    @BeforeEach
    void setUp() {
        // Mockeamos las dependencias
        loanTypeRepository = mock(LoanTypeRepository.class);
        requestDomainRepository = mock(RequestDomainRepository.class);
        userRepository = mock(UserRepository.class);
        // Creamos instancia de la clase que probaremos
        requestUseCase = new RequestUseCase(loanTypeRepository, requestDomainRepository, userRepository);
    }

    @Test
    void createRequestLoanTypeNotFound() {
        // Configuramos el repositorio para retornar "false"
        when(loanTypeRepository.loanTypeExistsById(1L))
                .thenReturn(Mono.just(false));

        // Creamos un DTO de ejemplo
        CreateRequestDomainDTO requestDTO = CreateRequestDomainDTO.builder()
                .typeLoanId(1L)
                .amount(BigDecimal.valueOf(5000))
                .term(24)
                .email("test@example.com")
                .documentNumber("456789")
                .build();

        // Ejecutamos el método y verificamos
        StepVerifier.create(requestUseCase.createRequest(requestDTO))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                                throwable.getMessage().equals("loan type not found")
                )
                .verify();

        // Verificamos que `saveRequestDomain` no se llamó
        verify(requestDomainRepository, never()).saveRequestDomain(any());
    }

    @Test
    void createRequestUserNotFound() {
        // Configuramos el repositorio para retornar "false"
        when(loanTypeRepository.loanTypeExistsById(1L))
                .thenReturn(Mono.just(true));
        when(userRepository.userExistsByDocumentNumber("456789")).thenReturn(Mono.just(false));
        // Creamos un DTO de ejemplo
        CreateRequestDomainDTO requestDTO = CreateRequestDomainDTO.builder()
                .typeLoanId(1L)
                .amount(BigDecimal.valueOf(5000))
                .term(24)
                .email("test@example.com")
                .documentNumber("456789")
                .build();

        // Ejecutamos el método y verificamos
        StepVerifier.create(requestUseCase.createRequest(requestDTO))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                                throwable.getMessage().equals("user not found")
                )
                .verify();

        // Verificamos que `saveRequestDomain` no se llamó
        verify(requestDomainRepository, never()).saveRequestDomain(any());
    }

    @Test
    void createRequestSuccess() {
        // Configuramos el repositorio para retornar "true"
        when(loanTypeRepository.loanTypeExistsById(1L))
                .thenReturn(Mono.just(true));
        when(userRepository.userExistsByDocumentNumber("456789")).thenReturn(Mono.just(true));

        // Configuramos el repositorio para simular el guardado
        when(requestDomainRepository.saveRequestDomain(any(RequestDomain.class)))
                .thenReturn(Mono.just(RequestDomain.builder()
                        .typeLoanId(1L)
                        .amount(BigDecimal.valueOf(5000))
                        .term(24)
                        .email("test@example.com")
                        .statusId(StatusEnum.PENDING.getId())
                        .build()));

        // Creamos un DTO de ejemplo
        CreateRequestDomainDTO requestDTO = CreateRequestDomainDTO.builder()
                .typeLoanId(1L)
                .amount(BigDecimal.valueOf(5000))
                .term(24)
                .email("test@example.com")
                .documentNumber("456789")
                .build();

        // Ejecutamos el método
        StepVerifier.create(requestUseCase.createRequest(requestDTO))
                .assertNext(requestDomain -> {
                    // Verificamos que los datos retornados son los esperados
                    assert requestDomain.getTypeLoanId().equals(1L);
                    assert requestDomain.getAmount().equals(BigDecimal.valueOf(5000));
                    assert requestDomain.getTerm().equals(24);
                    assert requestDomain.getEmail().equals("test@example.com");
                    assert requestDomain.getStatusId().equals(StatusEnum.PENDING.getId());
                })
                .verifyComplete();

        // Verificamos que `saveRequestDomain` se llamó una vez
        verify(requestDomainRepository, times(1)).saveRequestDomain(any(RequestDomain.class));
    }

    @Test
    @DisplayName("Enriquece: clientName, baseSalary y monthlyDebt cuando el usuario existe y status es APPROVED. Dedup de emails.")
    void enriches_and_setsMonthlyDebt_whenApproved_andUserFound_dedupEmails() {
        // Datos
        RequestBasicAdminInfo a1 = req("1", "a@x.com", APPROVED_ID, new BigDecimal("10000"), 2.0, 12);
        RequestBasicAdminInfo a2 = req("2", "a@x.com", APPROVED_ID, new BigDecimal("20000"), 2.5, 24); // email duplicado

        PageResult<RequestBasicAdminInfo> page = new PageResult<>(
                List.of(a1, a2), 0, 2, 2L, 1
        );

        User ua = user("Ana", "Doe", new BigDecimal("2500000"), "a@x.com");

        when(requestDomainRepository.findPage(anySet(), eq(0), eq(2)))
                .thenReturn(Mono.just(page));

        // Por defecto, que no encuentre otros correos
        when(userRepository.getUserByEmail(anyString())).thenReturn(Mono.empty());
        when(userRepository.getUserByEmail(eq("a@x.com"))).thenReturn(Mono.just(ua));

        try (MockedStatic<LoanMath> mocked = mockStatic(LoanMath.class)) {
            mocked.when(() -> LoanMath.monthlyPayment(any(BigDecimal.class), any(BigDecimal.class), anyInt(), eq(true)))
                    .thenReturn(new BigDecimal("123.45"));

            StepVerifier.create(requestUseCase.getListByStatus(Set.of(APPROVED_ID), 0, 2))
                    .assertNext(result -> {
                        assertEquals(0, result.page());
                        assertEquals(2, result.size());
                        assertEquals(2L, result.totalElements());
                        assertEquals(1, result.totalPages());
                        assertEquals(2, result.content().size());

                        // Ambos enriquecidos con el mismo user (dedup OK)
                        for (RequestBasicAdminInfo it : result.content()) {
                            assertEquals("Ana Doe", it.getClientName());
                            assertEquals(new BigDecimal("2500000"), it.getBaseSalary());
                            assertEquals(new BigDecimal("123.45"), it.getMonthlyDebt());
                        }
                    })
                    .verifyComplete();

            // Llamado solo una vez por email único
            verify(userRepository, times(1)).getUserByEmail("a@x.com");
            verifyNoMoreInteractions(userRepository);

            // Se llamó al cálculo al menos una vez (dos items, ambos APPROVED)
            mocked.verify(() -> LoanMath.monthlyPayment(any(BigDecimal.class), any(BigDecimal.class), anyInt(), eq(true)), times(2));
        }
    }

    @Test
    @DisplayName("NO setea monthlyDebt si status != APPROVED; clientName y baseSalary sí se enriquecen.")
    void doesNotSetMonthlyDebt_whenNotApproved() {
        // status distinto a APPROVED
        long NOT_APPROVED = APPROVED_ID + 9999L;

        RequestBasicAdminInfo b1 = req("3", "b@x.com", NOT_APPROVED, new BigDecimal("15000"), 3.1, 18);
        PageResult<RequestBasicAdminInfo> page = new PageResult<>(List.of(b1), 1, 1, 1L, 1);

        User ub = user("Bob", "Smith", new BigDecimal("1800000"), "b@x.com");

        when(requestDomainRepository.findPage(anySet(), eq(1), eq(1))).thenReturn(Mono.just(page));
        when(userRepository.getUserByEmail("b@x.com")).thenReturn(Mono.just(ub));

        try (MockedStatic<LoanMath> mocked = mockStatic(LoanMath.class)) {
            // No debería llamarse para NOT_APPROVED, pero si se llamara, devolvemos algún valor
            mocked.when(() -> LoanMath.monthlyPayment(any(), any(), anyInt(), anyBoolean()))
                    .thenReturn(new BigDecimal("777"));

            StepVerifier.create(requestUseCase.getListByStatus(Set.of(NOT_APPROVED), 1, 1))
                    .assertNext(result -> {
                        RequestBasicAdminInfo it = result.content().get(0);
                        assertEquals("Bob Smith", it.getClientName());
                        assertEquals(new BigDecimal("1800000"), it.getBaseSalary());
                        assertNull(it.getMonthlyDebt(), "monthlyDebt debe ser null cuando status != APPROVED");
                    })
                    .verifyComplete();

            // Asegura que NO se llamó al cálculo estático
            mocked.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("Ignora emails null; ignora errores por-email y usuarios no encontrados (Mono.empty()).")
    void ignoresNullEmails_andErrorPerEmail_andEmptyUser() {
        RequestBasicAdminInfo n1 = req("4", null, APPROVED_ID, new BigDecimal("12000"), 2.8, 10);        // null email
        RequestBasicAdminInfo n2 = req("5", "err@x.com", APPROVED_ID, new BigDecimal("9000"), 2.2, 8);   // error
        RequestBasicAdminInfo n3 = req("6", "empty@x.com", APPROVED_ID, new BigDecimal("5000"), 1.5, 6); // empty user

        PageResult<RequestBasicAdminInfo> page = new PageResult<>(List.of(n1, n2, n3), 0, 3, 3L, 1);

        when(requestDomainRepository.findPage(anySet(), eq(0), eq(3))).thenReturn(Mono.just(page));
        when(userRepository.getUserByEmail("err@x.com")).thenReturn(Mono.error(new RuntimeException("boom")));
        when(userRepository.getUserByEmail("empty@x.com")).thenReturn(Mono.empty());

        try (MockedStatic<LoanMath> mocked = mockStatic(LoanMath.class)) {
            // Aunque son APPROVED, si no hay usuario NO debe llamarse LoanMath (no se enriquece)
            mocked.when(() -> LoanMath.monthlyPayment(any(), any(), anyInt(), anyBoolean()))
                    .thenReturn(new BigDecimal("999"));

            StepVerifier.create(requestUseCase.getListByStatus(Set.of(APPROVED_ID), 0, 3))
                    .assertNext(result -> {
                        // Ninguno debe ser enriquecido porque:
                        // n1 email null -> no búsqueda
                        // n2 error -> ignorado
                        // n3 empty -> sin user
                        for (RequestBasicAdminInfo it : result.content()) {
                            assertNull(it.getClientName());
                            assertNull(it.getBaseSalary());
                            assertNull(it.getMonthlyDebt());
                        }
                    })
                    .verifyComplete();

            // Llamados realizados solo para correos no nulos
            verify(userRepository, times(1)).getUserByEmail("err@x.com");
            verify(userRepository, times(1)).getUserByEmail("empty@x.com");
            verifyNoMoreInteractions(userRepository);

            // No debería calcularse nada porque no hubo user encontrado
            mocked.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("Construye clientName correctamente: solo nombre, solo apellido, y ambos.")
    void buildsClientName_skippingNulls() {
        RequestBasicAdminInfo c1 = req("7", "onlyname@x.com", APPROVED_ID, new BigDecimal("8000"), 2.0, 8);
        RequestBasicAdminInfo c2 = req("8", "onlysurname@x.com", APPROVED_ID, new BigDecimal("7000"), 1.8, 7);
        RequestBasicAdminInfo c3 = req("9", "both@x.com", APPROVED_ID, new BigDecimal("6000"), 1.7, 6);

        PageResult<RequestBasicAdminInfo> page = new PageResult<>(List.of(c1, c2, c3), 2, 3, 3L, 1);

        User u1 = user("Charlie", null, new BigDecimal("1000000"), "onlyname@x.com");
        User u2 = user(null, "Solo", new BigDecimal("1100000"), "onlysurname@x.com");
        User u3 = user("Dana", "White", new BigDecimal("1200000"), "both@x.com");

        when(requestDomainRepository.findPage(anySet(), eq(2), eq(3))).thenReturn(Mono.just(page));
        when(userRepository.getUserByEmail("onlyname@x.com")).thenReturn(Mono.just(u1));
        when(userRepository.getUserByEmail("onlysurname@x.com")).thenReturn(Mono.just(u2));
        when(userRepository.getUserByEmail("both@x.com")).thenReturn(Mono.just(u3));

        try (MockedStatic<LoanMath> mocked = mockStatic(LoanMath.class)) {
            mocked.when(() -> LoanMath.monthlyPayment(any(), any(), anyInt(), eq(true)))
                    .thenReturn(new BigDecimal("321.00"));

            StepVerifier.create(requestUseCase.getListByStatus(Set.of(APPROVED_ID), 2, 3))
                    .assertNext(result -> {
                        Map<String, RequestBasicAdminInfo> byEmail = result.content()
                                .stream().collect(Collectors.toMap(RequestBasicAdminInfo::getEmail, x -> x));

                        assertEquals("Charlie", byEmail.get("onlyname@x.com").getClientName());
                        assertEquals("Solo", byEmail.get("onlysurname@x.com").getClientName());
                        assertEquals("Dana White", byEmail.get("both@x.com").getClientName());

                        // También setea baseSalary y monthlyDebt en todos (APPROVED)
                        result.content().forEach(it -> {
                            assertNotNull(it.getBaseSalary());
                            assertEquals(new BigDecimal("321.00"), it.getMonthlyDebt());
                        });
                    })
                    .verifyComplete();

            mocked.verify(() -> LoanMath.monthlyPayment(any(), any(), anyInt(), eq(true)), times(3));
        }
    }

    // -----------------------
    // Helpers
    // -----------------------

    private static RequestBasicAdminInfo req(String id, String email, Long statusId, BigDecimal amount, Double interestRate, Integer term) {
        RequestBasicAdminInfo r = new RequestBasicAdminInfo();
        r.setId(id);
        r.setEmail(email);
        r.setStatusId(statusId);
        r.setAmount(amount);
        r.setInterestRate(interestRate);
        r.setTerm(term);
        return r;
    }

    private static User user(String name, String surname, BigDecimal baseSalary, String email) {
        User u = new User();
        u.setName(name);
        u.setSurname(surname);
        u.setBaseSalary(baseSalary);
        u.setEmail(email);
        u.setBirthDate(LocalDate.of(1990, 1, 1));
        u.setAddress("addr");
        u.setPhone("123");
        u.setRoleId(1L);
        u.setDocumentNumber("DOC");
        return u;
    }

}