package com.bankingsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class LoanResponse {
    private Long id;
    private Long userId;
    private Long accountId;
    private String loanType;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer durationMonth;
    private BigDecimal monthlyPayment;
    private BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextPaymentDate;
    private Integer overdueDays;
    private String status;
}


