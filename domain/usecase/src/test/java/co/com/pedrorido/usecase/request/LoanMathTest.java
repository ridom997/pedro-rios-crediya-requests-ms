package co.com.pedrorido.usecase.request;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class LoanMathTest {

    @Test
    void monthlyPayment_withValidInputs_shouldCalculateCorrectly() {
        // Caso base: Cálculo con pagos mensuales fijos
        BigDecimal principal = BigDecimal.valueOf(10000);
        BigDecimal annualRate = BigDecimal.valueOf(0.05); // 5% anual
        int months = 12;
        boolean effectiveAnnual = false;

        BigDecimal result = LoanMath.monthlyPayment(principal, annualRate, months, effectiveAnnual);

        // Formula calculada manualmente
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(856.07));
    }

    @Test
    void monthlyPayment_withZeroInterest_shouldDividePrincipalByMonths() {
        // Caso con interés anual igual a 0
        BigDecimal principal = BigDecimal.valueOf(12000);
        BigDecimal annualRate = BigDecimal.ZERO; // 0% anual
        int months = 12;
        boolean effectiveAnnual = false;

        BigDecimal result = LoanMath.monthlyPayment(principal, annualRate, months, effectiveAnnual);

        // Resultado esperado: principal / meses
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
    }

    @Test
    void monthlyPayment_withEffectiveAnnualRate_shouldCalculateCorrectly() {
        // Prueba con tasa efectiva anual
        BigDecimal principal = BigDecimal.valueOf(10000);
        BigDecimal annualRate = BigDecimal.valueOf(0.12); // 12% efectivo anual
        int months = 12;
        boolean effectiveAnnual = true;

        BigDecimal result = LoanMath.monthlyPayment(principal, annualRate, months, effectiveAnnual);

        // Este cálculo es de validación específica
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(885.62));
    }

    @Test
    void monthlyPayment_withInvalidMonths_shouldThrowException() {
        // Cantidad de meses <= 0 debe lanzar excepción
        BigDecimal principal = BigDecimal.valueOf(10000);
        BigDecimal annualRate = BigDecimal.valueOf(0.12); // 12% efectivo anual
        int months = 0;
        boolean effectiveAnnual = true;

        assertThatThrownBy(() -> LoanMath.monthlyPayment(principal, annualRate, months, effectiveAnnual))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("months must be > 0");
    }

    @Test
    void dti_withValidInputs_shouldCalculateCorrectly() {
        // Caso base para cálculo de relación deuda/ingresos
        BigDecimal monthlyPayment = BigDecimal.valueOf(1000.00);
        BigDecimal salary = BigDecimal.valueOf(4000.00);

        BigDecimal result = LoanMath.dti(monthlyPayment, salary);

        // Resultado esperado: 1000 / 4000 = 0.2500
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(0.2500));
    }
}