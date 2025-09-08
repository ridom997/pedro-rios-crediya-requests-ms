package co.com.pedrorido.api.dto;

import lombok.*;

import java.math.BigDecimal;

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
    private Integer term;
    private Long typeLoanId;
}
