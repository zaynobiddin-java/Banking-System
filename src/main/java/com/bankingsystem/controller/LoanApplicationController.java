package com.bankingsystem.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.bankingsystem.dto.LoanApplicationRejectRequest;
import com.bankingsystem.dto.LoanApplicationRequest;
import com.bankingsystem.dto.LoanApplicationResponse;
import com.bankingsystem.service.LoanApplicationService;

import java.util.List;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
@Tag(name = "Loan Application")
public class LoanApplicationController {

    private final LoanApplicationService applicationService;

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> apply(@Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.ok(applicationService.apply(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LoanApplicationResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(applicationService.getByUser(userId));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<LoanApplicationResponse>> getByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(applicationService.getByCompany(companyId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanApplicationResponse>> getPending() {
        return ResponseEntity.ok(applicationService.getPending());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanApplicationResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanApplicationResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody LoanApplicationRejectRequest request) {
        return ResponseEntity.ok(applicationService.reject(id, request));
    }
}


