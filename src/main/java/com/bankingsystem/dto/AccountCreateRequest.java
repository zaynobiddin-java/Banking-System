package com.bankingsystem.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountCreateRequest {
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "3 harfli valyuta kodi bo'lishi kerak")
    private String currency;
}

