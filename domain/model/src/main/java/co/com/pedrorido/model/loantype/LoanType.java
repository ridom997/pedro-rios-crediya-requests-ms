package co.com.pedrorido.model.loantype;
import lombok.*;
//import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanType {
    private Long id;
    private String name;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private Double interestRate;
    private Boolean automaticValidation;
}
