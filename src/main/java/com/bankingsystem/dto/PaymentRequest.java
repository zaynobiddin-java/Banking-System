package com.bankingsystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotNull(message = "Jo'natuvchi hisob ID ko'rsatilishi shart")
    private Long fromAccountId;

    @NotNull(message = "Qabul qiluvchi hisob ID ko'rsatilishi shart")
    private Long toAccountId;

    @NotNull(message = "Summa ko'rsatilishi shart")
    @Positive(message = "Summa 0 dan katta bo'lishi kerak")
    private BigDecimal amount;
}


