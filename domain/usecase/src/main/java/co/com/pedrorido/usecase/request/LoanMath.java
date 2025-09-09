package co.com.pedrorido.usecase.request;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LoanMath {

    public static BigDecimal monthlyPayment(BigDecimal principal,
                                            BigDecimal annualRate,
                                            int months,
                                            boolean effectiveAnnual) {
        if (months <= 0) throw new IllegalArgumentException("months must be > 0");

        double P = principal.doubleValue();
        double iA = annualRate.doubleValue();
        double r = effectiveAnnual ? Math.pow(1.0 + iA, 1.0/12.0) - 1.0
                : iA / 12.0;

        double cuota = (r == 0.0)
                ? P / months
                : P * r / (1.0 - Math.pow(1.0 + r, -months));

        return BigDecimal.valueOf(cuota).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal dti(BigDecimal monthlyPayment, BigDecimal salary) {
        return monthlyPayment.divide(salary, 4, RoundingMode.HALF_UP); // 0.2365 = 23.65%
    }
}