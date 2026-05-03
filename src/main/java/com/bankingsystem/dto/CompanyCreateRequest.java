package com.bankingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CompanyCreateRequest {
    @NotBlank(message = "Kompaniya nomi bo'sh bo'lmasligi kerak")
    private String name;

    @NotBlank(message = "INN bo'sh bo'lmasligi kerak")
    @Pattern(regexp = "\\d{9}", message = "INN 9 xonali bo'lishi kerak")
    private String inn;

    @NotBlank(message = "Direktor ismi bo'sh bo'lmasligi kerak")
    private String director;
}
