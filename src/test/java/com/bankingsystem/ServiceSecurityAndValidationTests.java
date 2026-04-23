package com.bankingsystem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.bankingsystem.dto.AccountCreateRequest;
import com.bankingsystem.dto.CardCreateRequest;
import com.bankingsystem.dto.LoanCalculateRequest;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.AccountStatus;
import com.bankingsystem.entity.OwnerType;
import com.bankingsystem.entity.Role;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.UserStatus;
import com.bankingsystem.entity.UserType;
import com.bankingsystem.exception.BadRequestException;
import com.bankingsystem.exception.ForbiddenException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.service.AccountService;
import com.bankingsystem.service.CardService;
import com.bankingsystem.service.LoanService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ServiceSecurityAndValidationTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private CardService cardService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAccountRequiresAdminPrivileges() {
        User regularUser = saveUser(Role.USER);
        authenticate(regularUser);

        AccountCreateRequest request = new AccountCreateRequest();
        request.setCurrency("UZS");

        assertThatThrownBy(() -> accountService.createAccount(regularUser.getId(), request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("administrator");
    }

    @Test
    void getAccountByIdRejectsDifferentUser() {
        User owner = saveUser(Role.USER);
        User outsider = saveUser(Role.USER);
        Account account = saveAccount(owner, BigDecimal.TEN);
        authenticate(outsider);

        assertThatThrownBy(() -> accountService.getAccountById(account.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Hisobga kirish");
    }

    @Test
    void createCardRejectsMissingCardType() {
        User admin = saveUser(Role.ADMIN);
        Account account = saveAccount(admin, BigDecimal.ZERO);
        authenticate(admin);

        CardCreateRequest request = new CardCreateRequest();

        assertThatThrownBy(() -> cardService.createCard(account.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Karta turi");
    }

    @Test
    void calculateLoanRejectsMissingLoanType() {
        LoanCalculateRequest request = new LoanCalculateRequest();
        request.setAmount(new BigDecimal("1000000"));
        request.setDurationMonth(12);

        assertThatThrownBy(() -> loanService.calculate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Kredit turi");
    }

    private User saveUser(Role role) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .fullName("Test User " + suffix)
                .phone("+99890" + Math.abs(suffix.hashCode() % 10000000))
                .password("encoded-password")
                .passport("AA" + suffix.toUpperCase())
                .pinfl(String.format("%014d", Math.abs(suffix.hashCode())))
                .status(UserStatus.ACTIVE)
                .type(UserType.INDIVIDUAL)
                .role(role)
                .build());
    }

    private Account saveAccount(User owner, BigDecimal balance) {
        return accountRepository.save(Account.builder()
                .accountNumber("20208000900" + UUID.randomUUID().toString().replace("-", "").substring(0, 9))
                .balance(balance)
                .currency("UZS")
                .status(AccountStatus.ACTIVE)
                .ownerType(OwnerType.USER)
                .ownerId(owner.getId())
                .build());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getPhone(), null, List.of())
        );
    }
}
