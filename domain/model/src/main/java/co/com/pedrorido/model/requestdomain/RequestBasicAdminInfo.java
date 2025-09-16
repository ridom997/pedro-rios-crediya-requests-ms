package co.com.pedrorido.model.requestdomain;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class RequestBasicAdminInfo {
    private UUID id;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private String clientName;
    private Long typeLoanId;
    private Double interestRate;
    private BigDecimal baseSalary;
    private Long statusId;
    private BigDecimal monthlyDebt;
}
