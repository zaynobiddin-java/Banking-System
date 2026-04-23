package com.bankingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {
    @NotBlank(message = "Refresh token bo'sh bo'lmasligi kerak")
    private String refreshToken;
}


