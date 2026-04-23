package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bankingsystem.dto.LoanCalculateRequest;
import com.bankingsystem.dto.LoanCalculateResponse;
import com.bankingsystem.dto.LoanPayRequest;
import com.bankingsystem.dto.LoanPaymentResponse;
import com.bankingsystem.dto.LoanResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.AccountStatus;
import com.bankingsystem.entity.Loan;
import com.bankingsystem.entity.LoanPayment;
import com.bankingsystem.entity.LoanPaymentStatus;
import com.bankingsystem.entity.LoanStatus;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.entity.TransactionStatus;
import com.bankingsystem.entity.TransactionType;
import com.bankingsystem.exception.BadRequestException;
import com.bankingsystem.exception.NotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.LoanPaymentRepository;
import com.bankingsystem.repository.LoanRepository;
import com.bankingsystem.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanCalculator loanCalculator;
    private final AccessControlService accessControlService;

    public LoanCalculateResponse calculate(LoanCalculateRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Summa 0 dan katta bo'lishi kerak");
        }
        if (request.getDurationMonth() == null || request.getDurationMonth() < 1) {
            throw new BadRequestException("Muddat noto'g'ri");
        }
        if (request.getLoanType() == null) {
            throw new BadRequestException("Kredit turi ko'rsatilishi shart");
        }

        BigDecimal rate = loanCalculator.getInterestRate(request.getLoanType());
        BigDecimal monthlyPayment = loanCalculator.calculateMonthlyPayment(
                request.getAmount(), rate, request.getDurationMonth());
        BigDecimal totalAmount = loanCalculator.calculateTotalAmount(monthlyPayment, request.getDurationMonth());
        BigDecimal totalInterest = totalAmount.subtract(request.getAmount());

        return LoanCalculateResponse.builder()
                .amount(request.getAmount())
                .durationMonth(request.getDurationMonth())
                .interestRate(rate)
                .monthlyPayment(monthlyPayment)
                .totalAmount(totalAmount)
                .totalInterest(totalInterest)
                .build();
    }

    public List<LoanResponse> getByUser(Long userId) {
        accessControlService.requireUserAccess(userId);
        return loanRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toLoanResponse).toList();
    }

    public LoanResponse getById(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Kredit topilmadi"));
        accessControlService.requireLoanAccess(loan);
        return toLoanResponse(loan);
    }

    public List<LoanPaymentResponse> getSchedule(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Kredit topilmadi"));
        accessControlService.requireLoanAccess(loan);
        return loanPaymentRepository.findByLoanIdOrderByPaymentNumberAsc(loanId)
                .stream().map(this::toPaymentResponse).toList();
    }

    @Transactional
    public LoanResponse pay(Long loanId, LoanPayRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Summa 0 dan katta bo'lishi kerak");
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Kredit topilmadi"));

        accessControlService.requireLoanAccess(loan);

        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new BadRequestException("Kredit yopilgan");
        }

        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        accessControlService.requireAccountAccess(fromAccount);

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Hisob aktiv emas");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Hisobda yetarli mablag' yo'q");
        }

        List<LoanPayment> unpaidPayments = loanPaymentRepository
                .findByLoanIdOrderByPaymentNumberAsc(loanId)
                .stream()
                .filter(p -> p.getStatus() != LoanPaymentStatus.PAID)
                .toList();

        if (unpaidPayments.isEmpty()) {
            throw new BadRequestException("To'lanishi kerak bo'lgan summa yo'q");
        }

        BigDecimal remaining = request.getAmount();
        for (LoanPayment payment : unpaidPayments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal needed = payment.getTotalAmount().add(payment.getPenalty()).subtract(payment.getPaidAmount());
            BigDecimal toPay = remaining.min(needed);

            payment.setPaidAmount(payment.getPaidAmount().add(toPay));
            remaining = remaining.subtract(toPay);

            if (payment.getPaidAmount().compareTo(payment.getTotalAmount().add(payment.getPenalty())) >= 0) {
                payment.setStatus(LoanPaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
            } else {
                payment.setStatus(LoanPaymentStatus.PARTIAL);
            }

            loanPaymentRepository.save(payment);
        }

        BigDecimal actuallyPaid = request.getAmount().subtract(remaining);
        fromAccount.setBalance(fromAccount.getBalance().subtract(actuallyPaid));
        accountRepository.save(fromAccount);

        loan.setRemainingAmount(loan.getRemainingAmount().subtract(actuallyPaid));
        refreshLoanState(loan);
        loanRepository.save(loan);

        Transaction tx = Transaction.builder()
                .fromAccount(fromAccount)
                .toAccount(null)
                .amount(actuallyPaid)
                .fee(BigDecimal.ZERO)
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        return toLoanResponse(loan);
    }

    private LoanResponse toLoanResponse(Loan l) {
        return LoanResponse.builder()
                .id(l.getId())
                .userId(l.getUser().getId())
                .accountId(l.getAccount() != null ? l.getAccount().getId() : null)
                .loanType(l.getLoanType() != null ? l.getLoanType().name() : null)
                .amount(l.getAmount())
                .interestRate(l.getInterestRate())
                .durationMonth(l.getDurationMonth())
                .monthlyPayment(l.getMonthlyPayment())
                .totalAmount(l.getTotalAmount())
                .remainingAmount(l.getRemainingAmount())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .nextPaymentDate(l.getNextPaymentDate())
                .overdueDays(l.getOverdueDays())
                .status(l.getStatus().name())
                .build();
    }

    private LoanPaymentResponse toPaymentResponse(LoanPayment p) {
        return LoanPaymentResponse.builder()
                .id(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .dueDate(p.getDueDate())
                .paidAt(p.getPaidAt())
                .principal(p.getPrincipal())
                .interest(p.getInterest())
                .penalty(p.getPenalty())
                .totalAmount(p.getTotalAmount())
                .paidAmount(p.getPaidAmount())
                .status(p.getStatus().name())
                .build();
    }

    private void refreshLoanState(Loan loan) {
        if (loan.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setRemainingAmount(BigDecimal.ZERO);
            loan.setNextPaymentDate(null);
            loan.setOverdueDays(0);
            return;
        }

        LocalDate today = LocalDate.now();
        List<LoanPayment> payments = loanPaymentRepository.findByLoanIdOrderByPaymentNumberAsc(loan.getId());

        LoanPayment nextUnpaid = payments.stream()
                .filter(payment -> payment.getStatus() != LoanPaymentStatus.PAID)
                .findFirst()
                .orElse(null);

        LoanPayment overduePayment = payments.stream()
                .filter(payment -> payment.getStatus() != LoanPaymentStatus.PAID)
                .filter(payment -> payment.getDueDate().isBefore(today))
                .findFirst()
                .orElse(null);

        if (overduePayment != null) {
            loan.setStatus(LoanStatus.OVERDUE);
            loan.setOverdueDays((int) ChronoUnit.DAYS.between(overduePayment.getDueDate(), today));
            loan.setNextPaymentDate(overduePayment.getDueDate());
            return;
        }

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setOverdueDays(0);
        loan.setNextPaymentDate(nextUnpaid != null ? nextUnpaid.getDueDate() : null);
    }
}



