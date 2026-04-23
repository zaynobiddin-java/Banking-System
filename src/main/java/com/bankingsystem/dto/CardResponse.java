package com.bankingsystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardResponse {
    private Long id;
    private String cardNumber;
    private String expiry;
    private String type;
    private String status;
    private Long accountId;
}

