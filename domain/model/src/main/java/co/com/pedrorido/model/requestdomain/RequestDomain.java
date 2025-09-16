package co.com.pedrorido.model.requestdomain;
import lombok.*;
//import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
public class RequestDomain {
    private UUID id;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private Long statusId;
    private Long typeLoanId;
}
