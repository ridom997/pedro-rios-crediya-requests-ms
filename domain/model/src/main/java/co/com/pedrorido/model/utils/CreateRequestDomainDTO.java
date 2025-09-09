package co.com.pedrorido.model.utils;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CreateRequestDomainDTO {
    private String documentNumber;
    private String email;
    private BigDecimal amount;
    private Integer term;
    private Long typeLoanId;
}
