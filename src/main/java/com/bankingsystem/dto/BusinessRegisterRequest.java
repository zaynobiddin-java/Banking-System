package com.bankingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BusinessRegisterRequest {
    @NotBlank(message = "To'liq ism bo'sh bo'lmasligi kerak")
    private String fullName;

    @NotBlank(message = "Telefon raqam bo'sh bo'lmasligi kerak")
    @Pattern(regexp = "\\+?\\d{7,15}", message = "Telefon raqam formati noto'g'ri")
    private String phone;

    @NotBlank(message = "Parol bo'sh bo'lmasligi kerak")
    @Size(min = 6, message = "Parol kamida 6 ta belgidan iborat bo'lishi kerak")
    private String password;

    @NotBlank(message = "Passport bo'sh bo'lmasligi kerak")
    private String passport;

    @NotBlank(message = "PINFL bo'sh bo'lmasligi kerak")
    @Pattern(regexp = "\\d{14}", message = "PINFL 14 xonali bo'lishi kerak")
    private String pinfl;

    @NotBlank(message = "Kompaniya nomi bo'sh bo'lmasligi kerak")
    private String companyName;

    @NotBlank(message = "INN bo'sh bo'lmasligi kerak")
    @Pattern(regexp = "\\d{9}", message = "INN 9 xonali bo'lishi kerak")
    private String companyInn;

    @NotBlank(message = "Direktor ismi bo'sh bo'lmasligi kerak")
    private String director;
}
