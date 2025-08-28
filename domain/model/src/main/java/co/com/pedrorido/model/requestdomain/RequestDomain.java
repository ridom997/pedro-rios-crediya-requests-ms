package co.com.pedrorido.model.requestdomain;
import lombok.*;
//import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RequestDomain {
    private String id;
    private BigDecimal amount;
    private LocalDate term;
    private String email;
    private String statusId;
    private Long typeLoanId;
}
