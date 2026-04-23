package com.bankingsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanPaymentResponse {
    private Long id;
    private Integer paymentNumber;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal penalty;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String status;
}


