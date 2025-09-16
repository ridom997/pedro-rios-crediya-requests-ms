package co.com.pedrorido.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RequestResponseDTO {
    private UUID id;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private Long statusId;
    private Long typeLoanId;
}
