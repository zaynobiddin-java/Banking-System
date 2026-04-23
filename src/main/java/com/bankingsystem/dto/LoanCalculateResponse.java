package com.bankingsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoanCalculateResponse {
    private BigDecimal amount;
    private Integer durationMonth;
    private BigDecimal interestRate;
    private BigDecimal monthlyPayment;
    private BigDecimal totalAmount;
    private BigDecimal totalInterest;
}


