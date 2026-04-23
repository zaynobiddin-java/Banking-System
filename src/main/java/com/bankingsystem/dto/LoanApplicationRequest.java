package com.bankingsystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import com.bankingsystem.entity.LoanType;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    @NotNull(message = "Foydalanuvchi ID ko'rsatilishi shart")
    private Long userId;

    @NotNull(message = "Hisob ID ko'rsatilishi shart")
    private Long accountId;

    @NotNull(message = "Summa ko'rsatilishi shart")
    @Positive(message = "Summa 0 dan katta bo'lishi kerak")
    private BigDecimal amount;

    @NotNull(message = "Muddat ko'rsatilishi shart")
    @Min(value = 1, message = "Muddat kamida 1 oy bo'lishi kerak")
    @Max(value = 360, message = "Muddat 360 oydan oshmasligi kerak")
    private Integer durationMonth;

    @NotNull(message = "Kredit turi ko'rsatilishi shart")
    private LoanType loanType;

    @PositiveOrZero(message = "Oylik daromad manfiy bo'lmasligi kerak")
    private BigDecimal monthlyIncome;
}


