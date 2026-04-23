package com.bankingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoanApplicationRejectRequest {
    @NotBlank(message = "Rad etish sababi bo'sh bo'lmasligi kerak")
    private String reason;
}


