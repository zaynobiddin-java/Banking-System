package com.bankingsystem.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.bankingsystem.dto.AccountCreateRequest;
import com.bankingsystem.dto.AccountResponse;
import com.bankingsystem.dto.AccountUpdateRequest;
import com.bankingsystem.service.AccountService;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Account")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> createAccount(
            @PathVariable Long userId,
            @Valid @RequestBody AccountCreateRequest request) {
        return ResponseEntity.ok(accountService.createAccount(userId, request));
    }

    @PostMapping("/company/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> createCompanyAccount(
            @PathVariable Long companyId,
            @Valid @RequestBody AccountCreateRequest request) {
        return ResponseEntity.ok(accountService.createCompanyAccount(companyId, request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCompanyId(@PathVariable Long companyId) {
        return ResponseEntity.ok(accountService.getAccountsByCompanyId(companyId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @PutMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(accountId, request));
    }

    @PatchMapping("/{accountId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> freezeAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.freezeAccount(accountId));
    }

    @PatchMapping("/{accountId}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> unfreezeAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.unfreezeAccount(accountId));
    }

    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }
}

