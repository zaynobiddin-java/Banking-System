package com.bankingsystem.service;

import org.springframework.stereotype.Component;
import com.bankingsystem.entity.LoanType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class LoanCalculator {

    private static final int SCALE = 2;
    private static final MathContext MC = new MathContext(20);

    public BigDecimal getInterestRate(LoanType type) {
        return switch (type) {
            case MORTGAGE -> new BigDecimal("12");
            case AUTO -> new BigDecimal("18");
            case CONSUMER -> new BigDecimal("24");
            case BUSINESS -> new BigDecimal("22");
            case MICRO -> new BigDecimal("30");
        };
    }

    public BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate
                .divide(new BigDecimal("12"), MC)
                .divide(new BigDecimal("100"), MC);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount.divide(new BigDecimal(months), SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(months, MC);

        BigDecimal numerator = amount.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalAmount(BigDecimal monthlyPayment, int months) {
        return monthlyPayment.multiply(new BigDecimal(months)).setScale(SCALE, RoundingMode.HALF_UP);
    }
}


