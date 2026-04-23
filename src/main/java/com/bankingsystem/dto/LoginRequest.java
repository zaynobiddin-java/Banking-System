package com.bankingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Telefon raqam bo'sh bo'lmasligi kerak")
    private String phone;

    @NotBlank(message = "Parol bo'sh bo'lmasligi kerak")
    private String password;
}


