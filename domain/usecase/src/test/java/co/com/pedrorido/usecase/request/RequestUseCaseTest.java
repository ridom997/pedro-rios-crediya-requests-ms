package co.com.pedrorido.usecase.request;

import co.com.pedrorido.model.external.gateways.UserRepository;
import co.com.pedrorido.model.loantype.gateways.LoanTypeRepository;
import co.com.pedrorido.model.requestdomain.RequestDomain;
import co.com.pedrorido.model.requestdomain.gateways.RequestDomainRepository;
import co.com.pedrorido.model.utils.CreateRequestDomainDTO;
import co.com.pedrorido.model.utils.StatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.*;


class RequestUseCaseTest {
    private LoanTypeRepository loanTypeRepository;
    private RequestDomainRepository requestDomainRepository;
    private RequestUseCase requestUseCase;
    private UserRepository userRepository;

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
                .term(LocalDate.of(2990, 1, 15))
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
                .term(LocalDate.of(2990, 1, 15))
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
                        .term(LocalDate.of(2990, 1, 15))
                        .email("test@example.com")
                        .statusId(StatusEnum.PENDING.getDescription())
                        .build()));

        // Creamos un DTO de ejemplo
        CreateRequestDomainDTO requestDTO = CreateRequestDomainDTO.builder()
                .typeLoanId(1L)
                .amount(BigDecimal.valueOf(5000))
                .term(LocalDate.of(2990, 1, 15))
                .email("test@example.com")
                .documentNumber("456789")
                .build();

        // Ejecutamos el método
        StepVerifier.create(requestUseCase.createRequest(requestDTO))
                .assertNext(requestDomain -> {
                    // Verificamos que los datos retornados son los esperados
                    assert requestDomain.getTypeLoanId().equals(1L);
                    assert requestDomain.getAmount().equals(BigDecimal.valueOf(5000));
                    assert requestDomain.getTerm().equals(LocalDate.of(2990, 1, 15));
                    assert requestDomain.getEmail().equals("test@example.com");
                    assert requestDomain.getStatusId().equals(StatusEnum.PENDING.getDescription());
                })
                .verifyComplete();

        // Verificamos que `saveRequestDomain` se llamó una vez
        verify(requestDomainRepository, times(1)).saveRequestDomain(any(RequestDomain.class));
    }

}