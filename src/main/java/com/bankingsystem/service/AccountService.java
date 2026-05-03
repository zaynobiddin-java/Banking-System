package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.bankingsystem.dto.AccountCreateRequest;
import com.bankingsystem.dto.AccountResponse;
import com.bankingsystem.dto.AccountUpdateRequest;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.AccountStatus;
import com.bankingsystem.entity.OwnerType;
import com.bankingsystem.exception.BadRequestException;
import com.bankingsystem.exception.NotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.CompanyRepository;
import com.bankingsystem.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AccessControlService accessControlService;

    public AccountResponse createAccount(Long userId, AccountCreateRequest request) {
        accessControlService.requireAdmin();

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Foydalanuvchi topilmadi");
        }

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .currency(normalizeCurrency(request.getCurrency()))
                .status(AccountStatus.ACTIVE)
                .ownerType(OwnerType.USER)
                .ownerId(userId)
                .build();

        accountRepository.save(account);

        return toResponse(account);
    }

    public AccountResponse createCompanyAccount(Long companyId, AccountCreateRequest request) {
        accessControlService.requireAdmin();

        if (!companyRepository.existsById(companyId)) {
            throw new NotFoundException("Kompaniya topilmadi");
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .balance(BigDecimal.ZERO)
                .currency(normalizeCurrency(request.getCurrency()))
                .status(AccountStatus.ACTIVE)
                .ownerType(OwnerType.COMPANY)
                .ownerId(companyId)
                .build();

        accountRepository.save(account);
        return toResponse(account);
    }

    public List<AccountResponse> getAccountsByUserId(Long userId) {
        accessControlService.requireUserAccess(userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Foydalanuvchi topilmadi");
        }

        return accountRepository.findByOwnerIdAndOwnerType(userId, OwnerType.USER)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AccountResponse> getAccountsByCompanyId(Long companyId) {
        accessControlService.requireCompanyAccess(companyId);

        if (!companyRepository.existsById(companyId)) {
            throw new NotFoundException("Kompaniya topilmadi");
        }

        return accountRepository.findByOwnerIdAndOwnerType(companyId, OwnerType.COMPANY)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        accessControlService.requireAccountAccess(account);
        return toResponse(account);
    }

    public AccountResponse updateAccount(Long accountId, AccountUpdateRequest request) {
        accessControlService.requireAdmin();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BadRequestException("Yopilgan hisobni tahrirlash mumkin emas");
        }

        if (request.getCurrency() != null) {
            account.setCurrency(normalizeCurrency(request.getCurrency()));
        }

        accountRepository.save(account);
        return toResponse(account);
    }

    public AccountResponse freezeAccount(Long accountId) {
        accessControlService.requireAdmin();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BadRequestException("Yopilgan hisobni muzlatish mumkin emas");
        }

        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);
        return toResponse(account);
    }

    public AccountResponse unfreezeAccount(Long accountId) {
        accessControlService.requireAdmin();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new BadRequestException("Faqat muzlatilgan hisobni aktivlashtirish mumkin");
        }

        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
        return toResponse(account);
    }

    public void deleteAccount(Long accountId) {
        accessControlService.requireAdmin();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException("Balansda pul bor, hisobni yopish mumkin emas");
        }

        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .ownerType(account.getOwnerType().name())
                .ownerId(account.getOwnerId())
                .build();
    }

    private String generateAccountNumber() {
        Random random = new Random();
        String accountNumber;
        do {
            StringBuilder sb = new StringBuilder("20208000900");
            for (int i = 0; i < 9; i++) {
                sb.append(random.nextInt(10));
            }
            accountNumber = sb.toString();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "UZS";
        }

        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
        if (!normalizedCurrency.matches("[A-Z]{3}")) {
            throw new BadRequestException("Valyuta kodi 3 harfdan iborat bo'lishi kerak");
        }

        return normalizedCurrency;
    }
}

