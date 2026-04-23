package com.bankingsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.bankingsystem.entity.CardType;

@Data
public class CardCreateRequest {
    @NotNull(message = "Karta turi ko'rsatilishi shart")
    private CardType type;
}

