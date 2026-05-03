package com.bankingsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanApplicationResponse {
    private Long id;
    private Long userId;
    private Long companyId;
    private Long accountId;
    private BigDecimal amount;
    private Integer durationMonth;
    private String loanType;
    private BigDecimal monthlyIncome;
    private String status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}


