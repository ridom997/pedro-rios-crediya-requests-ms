package co.com.pedrorido.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CreateRequestDTO {
    private String documentNumber;
    private String email;
    private BigDecimal amount;
    private LocalDate term;
    private Long typeLoanId;
}
