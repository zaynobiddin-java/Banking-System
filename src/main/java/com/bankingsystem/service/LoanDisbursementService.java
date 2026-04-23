package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.Loan;
import com.bankingsystem.entity.LoanApplication;
import com.bankingsystem.entity.LoanPayment;
import com.bankingsystem.entity.LoanPaymentStatus;
import com.bankingsystem.entity.LoanStatus;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.entity.TransactionStatus;
import com.bankingsystem.entity.TransactionType;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.LoanPaymentRepository;
import com.bankingsystem.repository.LoanRepository;
import com.bankingsystem.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoanDisbursementService {

    private final LoanRepository loanRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanCalculator loanCalculator;

    @Transactional
    public Loan disburse(LoanApplication application) {
        BigDecimal rate = loanCalculator.getInterestRate(application.getLoanType());
        BigDecimal monthlyPayment = loanCalculator.calculateMonthlyPayment(
                application.getAmount(), rate, application.getDurationMonth());
        BigDecimal totalAmount = loanCalculator.calculateTotalAmount(monthlyPayment, application.getDurationMonth());

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(application.getDurationMonth());
        LocalDate firstPaymentDate = startDate.plusMonths(1);

        Loan loan = Loan.builder()
                .user(application.getUser())
                .account(application.getDisbursementAccount())
                .loanType(application.getLoanType())
                .amount(application.getAmount())
                .interestRate(rate)
                .durationMonth(application.getDurationMonth())
                .monthlyPayment(monthlyPayment)
                .totalAmount(totalAmount)
                .remainingAmount(totalAmount)
                .startDate(startDate)
                .endDate(endDate)
                .nextPaymentDate(firstPaymentDate)
                .overdueDays(0)
                .status(LoanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        loanRepository.save(loan);

        generateSchedule(loan, rate);

        Account account = application.getDisbursementAccount();
        account.setBalance(account.getBalance().add(application.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .fromAccount(null)
                .toAccount(account)
                .amount(application.getAmount())
                .fee(BigDecimal.ZERO)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(transaction);

        return loan;
    }

    private void generateSchedule(Loan loan, BigDecimal annualRate) {
        BigDecimal monthlyRate = annualRate
                .divide(new BigDecimal("12"), 20, RoundingMode.HALF_UP)
                .divide(new BigDecimal("100"), 20, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = loan.getAmount();
        BigDecimal monthlyPayment = loan.getMonthlyPayment();

        for (int i = 1; i <= loan.getDurationMonth(); i++) {
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principal = monthlyPayment.subtract(interest);

            if (i == loan.getDurationMonth()) {
                principal = remainingPrincipal;
            }

            remainingPrincipal = remainingPrincipal.subtract(principal);

            LoanPayment payment = LoanPayment.builder()
                    .loan(loan)
                    .paymentNumber(i)
                    .dueDate(loan.getStartDate().plusMonths(i))
                    .principal(principal)
                    .interest(interest)
                    .penalty(BigDecimal.ZERO)
                    .totalAmount(principal.add(interest))
                    .paidAmount(BigDecimal.ZERO)
                    .status(LoanPaymentStatus.SCHEDULED)
                    .build();

            loanPaymentRepository.save(payment);
        }
    }
}



