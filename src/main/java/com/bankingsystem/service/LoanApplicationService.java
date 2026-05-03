package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bankingsystem.dto.LoanApplicationRejectRequest;
import com.bankingsystem.dto.LoanApplicationRequest;
import com.bankingsystem.dto.LoanApplicationResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.AccountStatus;
import com.bankingsystem.entity.Company;
import com.bankingsystem.entity.LoanApplication;
import com.bankingsystem.entity.LoanApplicationStatus;
import com.bankingsystem.entity.OwnerType;
import com.bankingsystem.entity.User;
import com.bankingsystem.exception.BadRequestException;
import com.bankingsystem.exception.NotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.CompanyRepository;
import com.bankingsystem.repository.LoanApplicationRepository;
import com.bankingsystem.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private static final BigDecimal MAX_PAYMENT_TO_INCOME_RATIO = new BigDecimal("0.5");

    private final LoanApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final LoanCalculator loanCalculator;
    private final LoanDisbursementService loanDisbursementService;
    private final AccessControlService accessControlService;

    @Transactional
    public LoanApplicationResponse apply(LoanApplicationRequest request) {
        validateRequest(request);
        accessControlService.requireUserAccess(request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Foydalanuvchi topilmadi"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Hisob aktiv emas");
        }

        Company company = null;
        if (request.getCompanyId() != null) {
            company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new NotFoundException("Kompaniya topilmadi"));

            accessControlService.requireCompanyAccess(company.getId());

            if (user.getCompany() == null || !user.getCompany().getId().equals(company.getId())) {
                throw new BadRequestException("Foydalanuvchi kompaniyaga biriktirilmagan");
            }

            if (!account.getOwnerId().equals(company.getId()) || account.getOwnerType() != OwnerType.COMPANY) {
                throw new BadRequestException("Hisob kompaniyaga tegishli emas");
            }
        } else if (!account.getOwnerId().equals(user.getId()) || account.getOwnerType() != OwnerType.USER) {
            throw new BadRequestException("Hisob foydalanuvchiga tegishli emas");
        }

        BigDecimal rate = loanCalculator.getInterestRate(request.getLoanType());
        BigDecimal monthlyPayment = loanCalculator.calculateMonthlyPayment(
                request.getAmount(), rate, request.getDurationMonth());

        if (request.getMonthlyIncome() != null
                && monthlyPayment.compareTo(request.getMonthlyIncome().multiply(MAX_PAYMENT_TO_INCOME_RATIO)) > 0) {
            throw new BadRequestException("Oylik to'lov daromadning 50% dan oshmasligi kerak");
        }

        LoanApplication application = LoanApplication.builder()
                .user(user)
                .company(company)
                .disbursementAccount(account)
                .amount(request.getAmount())
                .durationMonth(request.getDurationMonth())
                .loanType(request.getLoanType())
                .monthlyIncome(request.getMonthlyIncome())
                .status(LoanApplicationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        applicationRepository.save(application);
        return toResponse(application);
    }

    @Transactional
    public LoanApplicationResponse approve(Long applicationId) {
        accessControlService.requireAdmin();

        LoanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Ariza topilmadi"));

        if (application.getStatus() != LoanApplicationStatus.PENDING) {
            throw new BadRequestException("Faqat PENDING statusdagi arizani tasdiqlash mumkin");
        }

        application.setStatus(LoanApplicationStatus.APPROVED);
        application.setProcessedAt(LocalDateTime.now());
        applicationRepository.save(application);

        loanDisbursementService.disburse(application);

        return toResponse(application);
    }

    @Transactional
    public LoanApplicationResponse reject(Long applicationId, LoanApplicationRejectRequest request) {
        accessControlService.requireAdmin();

        LoanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Ariza topilmadi"));

        if (application.getStatus() != LoanApplicationStatus.PENDING) {
            throw new BadRequestException("Faqat PENDING statusdagi arizani rad etish mumkin");
        }

        application.setStatus(LoanApplicationStatus.REJECTED);
        application.setRejectionReason(request.getReason());
        application.setProcessedAt(LocalDateTime.now());
        applicationRepository.save(application);

        return toResponse(application);
    }

    public List<LoanApplicationResponse> getByUser(Long userId) {
        accessControlService.requireUserAccess(userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Foydalanuvchi topilmadi");
        }
        return applicationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    public List<LoanApplicationResponse> getByCompany(Long companyId) {
        accessControlService.requireCompanyAccess(companyId);

        if (!companyRepository.existsById(companyId)) {
            throw new NotFoundException("Kompaniya topilmadi");
        }

        return applicationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream().map(this::toResponse).toList();
    }

    public List<LoanApplicationResponse> getPending() {
        accessControlService.requireAdmin();
        return applicationRepository.findByStatusOrderByCreatedAtDesc(LoanApplicationStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    private void validateRequest(LoanApplicationRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Summa 0 dan katta bo'lishi kerak");
        }
        if (request.getDurationMonth() == null || request.getDurationMonth() < 1 || request.getDurationMonth() > 360) {
            throw new BadRequestException("Muddat 1 oydan 360 oygacha bo'lishi kerak");
        }
        if (request.getLoanType() == null) {
            throw new BadRequestException("Kredit turi ko'rsatilishi shart");
        }
        if (request.getMonthlyIncome() != null && request.getMonthlyIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Oylik daromad manfiy bo'lishi mumkin emas");
        }
    }

    private LoanApplicationResponse toResponse(LoanApplication a) {
        return LoanApplicationResponse.builder()
                .id(a.getId())
                .userId(a.getUser().getId())
                .companyId(a.getCompany() != null ? a.getCompany().getId() : null)
                .accountId(a.getDisbursementAccount() != null ? a.getDisbursementAccount().getId() : null)
                .amount(a.getAmount())
                .durationMonth(a.getDurationMonth())
                .loanType(a.getLoanType().name())
                .monthlyIncome(a.getMonthlyIncome())
                .status(a.getStatus().name())
                .rejectionReason(a.getRejectionReason())
                .createdAt(a.getCreatedAt())
                .processedAt(a.getProcessedAt())
                .build();
    }
}


