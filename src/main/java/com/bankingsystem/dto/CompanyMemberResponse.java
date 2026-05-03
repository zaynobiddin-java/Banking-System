package com.bankingsystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyMemberResponse {
    private Long id;
    private Long companyId;
    private String fullName;
    private String phone;
    private String status;
    private String userType;
    private String role;
}
