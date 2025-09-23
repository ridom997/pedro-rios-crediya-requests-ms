package co.com.pedrorido.usecase.request;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
class LoanMathTest {

    // ===== monthlyPayment: casos felices =====

    @Test
    void monthlyPayment_zeroRate_dividesEvenly() {
        // P = 100000, i = 0, n = 12 -> 100000/12 con HALF_EVEN a 2 decimales
        BigDecimal out = LoanMath.monthlyPayment(
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                12
        );
        assertEquals(new BigDecimal("8333.33"), out);
    }

    @Test
    void monthlyPayment_positiveRate_happyPath() {
        // P = 100000, i = 0.02, n = 12 -> valor esperado con HALF_EVEN a 2 decimales
        BigDecimal out = LoanMath.monthlyPayment(
                new BigDecimal("100000"),
                new BigDecimal("0.02"),
                12
        );
        assertEquals(new BigDecimal("9455.96"), out);
    }

    @Test
    void monthlyPayment_negativeRate_isComputed() {
        // Tasa negativa no está prohibida por el método
        BigDecimal out = LoanMath.monthlyPayment(
                new BigDecimal("100000"),
                new BigDecimal("-0.01"),
                12
        );
        assertEquals(new BigDecimal("7801.64"), out);
    }

    // ===== monthlyPayment: validaciones / excepciones =====

    @Test
    void monthlyPayment_nullAmount_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                LoanMath.monthlyPayment(
                        null,
                        new BigDecimal("0.01"),
                        12
                )
        );
        assertTrue(ex.getMessage().contains("amount y monthlyRate no pueden ser null"));
    }

    @Test
    void monthlyPayment_nullMonthlyRate_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                LoanMath.monthlyPayment(
                        new BigDecimal("1000"),
                        null,
                        12
                )
        );
        assertTrue(ex.getMessage().contains("amount y monthlyRate no pueden ser null"));
    }

    @Test
    void monthlyPayment_nonPositiveAmount_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                LoanMath.monthlyPayment(
                        BigDecimal.ZERO, // <= 0
                        new BigDecimal("0.01"),
                        12
                )
        );
        assertTrue(ex.getMessage().contains("amount debe ser > 0"));
    }

    @Test
    void monthlyPayment_nonPositiveTerm_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                LoanMath.monthlyPayment(
                        new BigDecimal("1000"),
                        new BigDecimal("0.01"),
                        0
                )
        );
        assertTrue(ex.getMessage().contains("termMonths debe ser > 0"));
    }

    // ===== dti =====

    @Test
    void dti_happyPath_halfUpTo4Decimals() {
        // monthlyPayment = 2496.21, salary = 10000 -> 0.249621 ~ 0.2496 (4 decimales, HALF_UP)
        BigDecimal out = LoanMath.dti(new BigDecimal("2496.21"), new BigDecimal("10000"));
        assertEquals(new BigDecimal("0.2496"), out);
    }

    @Test
    void dti_divideByZero_throwsArithmeticException() {
        assertThrows(ArithmeticException.class, () ->
                LoanMath.dti(new BigDecimal("100.00"), BigDecimal.ZERO)
        );
    }
}