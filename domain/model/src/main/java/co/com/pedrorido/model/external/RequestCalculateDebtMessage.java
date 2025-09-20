package co.com.pedrorido.model.external;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public record RequestCalculateDebtMessage(UUID transactionId, String email, Date when, BigDecimal loanAmount,
                                          BigDecimal baseSalary,
                                          BigDecimal newMonthlyDebt, BigDecimal availableClientCapacity) {
}
