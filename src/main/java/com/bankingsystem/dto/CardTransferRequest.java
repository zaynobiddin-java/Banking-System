package com.bankingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardTransferRequest {
    @NotBlank(message = "Jo'natuvchi karta raqami bo'sh bo'lmasligi kerak")
    @Pattern(regexp = "\\d{16}", message = "Karta raqami 16 xonali bo'lishi kerak")
    private String fromCardNumber;

    @NotBlank(message = "Qabul qiluvchi karta raqami bo'sh bo'lmasligi kerak")
    @Pattern(regexp = "\\d{16}", message = "Karta raqami 16 xonali bo'lishi kerak")
    private String toCardNumber;

    @NotNull(message = "Summa ko'rsatilishi shart")
    @Positive(message = "Summa 0 dan katta bo'lishi kerak")
    private BigDecimal amount;
}


