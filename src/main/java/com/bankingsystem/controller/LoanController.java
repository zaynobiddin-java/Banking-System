package com.bankingsystem.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bankingsystem.dto.LoanCalculateRequest;
import com.bankingsystem.dto.LoanCalculateResponse;
import com.bankingsystem.dto.LoanPayRequest;
import com.bankingsystem.dto.LoanPaymentResponse;
import com.bankingsystem.dto.LoanResponse;
import com.bankingsystem.service.LoanService;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loan")
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/calculate")
    public ResponseEntity<LoanCalculateResponse> calculate(@Valid @RequestBody LoanCalculateRequest request) {
        return ResponseEntity.ok(loanService.calculate(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LoanResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(loanService.getByUser(userId));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<LoanResponse>> getByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(loanService.getByCompany(companyId));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<LoanResponse> getById(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.getById(loanId));
    }

    @GetMapping("/{loanId}/schedule")
    public ResponseEntity<List<LoanPaymentResponse>> getSchedule(@PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.getSchedule(loanId));
    }

    @PostMapping("/{loanId}/pay")
    public ResponseEntity<LoanResponse> pay(
            @PathVariable Long loanId,
            @Valid @RequestBody LoanPayRequest request) {
        return ResponseEntity.ok(loanService.pay(loanId, request));
    }
}


