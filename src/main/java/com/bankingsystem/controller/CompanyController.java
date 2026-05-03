package com.bankingsystem.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.bankingsystem.dto.CompanyCreateRequest;
import com.bankingsystem.dto.CompanyMemberCreateRequest;
import com.bankingsystem.dto.CompanyMemberResponse;
import com.bankingsystem.dto.CompanyResponse;
import com.bankingsystem.dto.CompanyUpdateRequest;
import com.bankingsystem.service.CompanyService;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Company")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody CompanyCreateRequest request) {
        return ResponseEntity.ok(companyService.createCompany(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CompanyResponse>> getCompanies() {
        return ResponseEntity.ok(companyService.getCompanies());
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(companyService.getCompany(companyId));
    }

    @PutMapping("/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable Long companyId,
            @RequestBody CompanyUpdateRequest request) {
        return ResponseEntity.ok(companyService.updateCompany(companyId, request));
    }

    @GetMapping("/{companyId}/members")
    public ResponseEntity<List<CompanyMemberResponse>> getMembers(@PathVariable Long companyId) {
        return ResponseEntity.ok(companyService.getMembers(companyId));
    }

    @PostMapping("/{companyId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyMemberResponse> createMember(
            @PathVariable Long companyId,
            @Valid @RequestBody CompanyMemberCreateRequest request) {
        return ResponseEntity.ok(companyService.createMember(companyId, request));
    }
}
