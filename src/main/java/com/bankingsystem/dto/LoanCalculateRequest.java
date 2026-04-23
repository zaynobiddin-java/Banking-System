package com.bankingsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import com.bankingsystem.entity.LoanType;

import java.math.BigDecimal;

@Data
public class LoanCalculateRequest {
    @NotNull(message = "Summa ko'rsatilishi shart")
    @Positive(message = "Summa 0 dan katta bo'lishi kerak")
    private BigDecimal amount;

    @NotNull(message = "Muddat ko'rsatilishi shart")
    @Min(value = 1, message = "Muddat kamida 1 oy bo'lishi kerak")
    private Integer durationMonth;

    @NotNull(message = "Kredit turi ko'rsatilishi shart")
    private LoanType loanType;
}


