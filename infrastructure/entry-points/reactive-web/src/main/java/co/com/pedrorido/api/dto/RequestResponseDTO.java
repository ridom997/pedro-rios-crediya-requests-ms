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
public class RequestResponseDTO {
    private String id;
    private BigDecimal amount;
    private LocalDate term;
    private String email;
    private String statusId;
    private Long typeLoanId;
}
