package co.com.pedrorido.usecase.request;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class LoanMath {

    public static BigDecimal monthlyPayment(BigDecimal amount,
                                            BigDecimal monthlyRate,
                                            int termMonths,
                                            boolean effectiveAnnual) {
        if (amount == null || monthlyRate == null) {
            throw new IllegalArgumentException("amount y monthlyRate no pueden ser null");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount debe ser > 0");
        }
        if (termMonths <= 0) {
            throw new IllegalArgumentException("termMonths debe ser > 0");
        }

        // Alta precisión para los cálculos intermedios
        MathContext mc = new MathContext(34, RoundingMode.HALF_EVEN); // DECIMAL128

        // Caso especial: i = 0  -> cuota = P / n
        if (monthlyRate.signum() == 0) {
            return amount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_EVEN);
        }

        BigDecimal one = BigDecimal.ONE;
        BigDecimal onePlusI = one.add(monthlyRate, mc);
        BigDecimal pow = onePlusI.pow(termMonths, mc);                // (1 + i)^n

        BigDecimal numerator = amount.multiply(monthlyRate, mc)        // P * i * (1+i)^n
                .multiply(pow, mc);

        BigDecimal denominator = pow.subtract(one, mc);                // (1+i)^n - 1

        // División final a escala deseada
        BigDecimal divide = numerator.divide(denominator, 2, RoundingMode.HALF_EVEN);
        return divide;
    }

    public static BigDecimal dti(BigDecimal monthlyPayment, BigDecimal salary) {
        return monthlyPayment.divide(salary, 4, RoundingMode.HALF_UP); // 0.2365 = 23.65%
    }
}