package co.com.pedrorido.model.utils;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CreateRequestDomainDTO {
    private String documentNumber;
    private String email;
    private BigDecimal amount;
    private LocalDate term;
    private Long typeLoanId;
}
