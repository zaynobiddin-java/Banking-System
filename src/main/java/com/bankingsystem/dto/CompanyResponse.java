package com.bankingsystem.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyResponse {
    private Long id;
    private String name;
    private String inn;
    private String director;
    private Long memberCount;
}
