package co.com.pedrorido.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RequestResponseDTO {
    private String id;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private Long statusId;
    private Long typeLoanId;
}
