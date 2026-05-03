package com.bankingsystem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.bankingsystem.dto.AccountResponse;
import com.bankingsystem.dto.BusinessRegisterRequest;
import com.bankingsystem.dto.LoanApplicationRequest;
import com.bankingsystem.dto.LoanApplicationResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.AccountStatus;
import com.bankingsystem.entity.Company;
import com.bankingsystem.entity.LoanType;
import com.bankingsystem.entity.OwnerType;
import com.bankingsystem.entity.Role;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.UserStatus;
import com.bankingsystem.entity.UserType;
import com.bankingsystem.exception.ForbiddenException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.CompanyRepository;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.service.AccountService;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.service.LoanApplicationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BusinessFeatureIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AccountRepository accountRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerBusinessCreatesLinkedCompanyUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);

        BusinessRegisterRequest request = new BusinessRegisterRequest();
        request.setFullName("Business User " + suffix);
        request.setPhone("+99891" + String.format("%07d", Math.abs(suffix.hashCode()) % 10000000));
        request.setPassword("secret123");
        request.setPassport("AB1234567");
        request.setPinfl("12345678901234");
        request.setCompanyName("Acme " + suffix);
        request.setCompanyInn(String.format("%09d", Math.abs(suffix.hashCode()) % 1000000000));
        request.setDirector("Director " + suffix);

        var response = authService.registerBusiness(request);

        User savedUser = userRepository.findByPhone(request.getPhone()).orElseThrow();

        assertThat(response.getUserType()).isEqualTo("BUSINESS");
        assertThat(response.getCompanyId()).isNotNull();
        assertThat(savedUser.getType()).isEqualTo(UserType.BUSINESS);
        assertThat(savedUser.getCompany()).isNotNull();
        assertThat(companyRepository.existsById(response.getCompanyId())).isTrue();
    }

    @Test
    void companyMemberCanReadCompanyOwnedAccount() {
        Company company = saveCompany();
        User businessUser = saveUser(Role.USER, UserType.BUSINESS, company);
        Account account = saveCompanyAccount(company, BigDecimal.valueOf(500_000));
        authenticate(businessUser);

        AccountResponse response = accountService.getAccountById(account.getId());

        assertThat(response.getId()).isEqualTo(account.getId());
        assertThat(response.getOwnerType()).isEqualTo(OwnerType.COMPANY.name());
        assertThat(response.getOwnerId()).isEqualTo(company.getId());
    }

    @Test
    void outsiderCannotReadCompanyOwnedAccount() {
        Company company = saveCompany();
        User outsider = saveUser(Role.USER, UserType.INDIVIDUAL, null);
        Account account = saveCompanyAccount(company, BigDecimal.valueOf(50_000));
        authenticate(outsider);

        assertThatThrownBy(() -> accountService.getAccountById(account.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Hisobga kirish");
    }

    @Test
    void companyLoanApplicationAcceptsCompanyOwnedAccount() {
        Company company = saveCompany();
        User businessUser = saveUser(Role.USER, UserType.BUSINESS, company);
        Account account = saveCompanyAccount(company, BigDecimal.ZERO);
        authenticate(businessUser);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setUserId(businessUser.getId());
        request.setCompanyId(company.getId());
        request.setAccountId(account.getId());
        request.setAmount(new BigDecimal("10000000"));
        request.setDurationMonth(12);
        request.setLoanType(LoanType.BUSINESS);
        request.setMonthlyIncome(new BigDecimal("5000000"));

        LoanApplicationResponse response = loanApplicationService.apply(request);

        assertThat(response.getCompanyId()).isEqualTo(company.getId());
        assertThat(response.getUserId()).isEqualTo(businessUser.getId());
        assertThat(response.getAccountId()).isEqualTo(account.getId());
    }

    private Company saveCompany() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        return companyRepository.save(Company.builder()
                .name("Company " + suffix)
                .inn(String.format("%09d", Math.abs(suffix.hashCode()) % 1000000000))
                .director("Director " + suffix)
                .build());
    }

    private User saveUser(Role role, UserType userType, Company company) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .fullName("Test User " + suffix)
                .phone("+99890" + String.format("%07d", Math.abs(suffix.hashCode()) % 10000000))
                .password("encoded-password")
                .passport("AA" + suffix.toUpperCase())
                .pinfl(String.format("%014d", Math.abs(suffix.hashCode())))
                .status(UserStatus.ACTIVE)
                .type(userType)
                .role(role)
                .company(company)
                .build());
    }

    private Account saveCompanyAccount(Company company, BigDecimal balance) {
        return accountRepository.save(Account.builder()
                .accountNumber("20208000900" + UUID.randomUUID().toString().replace("-", "").substring(0, 9))
                .balance(balance)
                .currency("UZS")
                .status(AccountStatus.ACTIVE)
                .ownerType(OwnerType.COMPANY)
                .ownerId(company.getId())
                .build());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getPhone(), null, List.of())
        );
    }
}
