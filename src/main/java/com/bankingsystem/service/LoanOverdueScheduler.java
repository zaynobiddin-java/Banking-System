package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.bankingsystem.entity.LoanPayment;
import com.bankingsystem.entity.LoanPaymentStatus;
import com.bankingsystem.entity.LoanStatus;
import com.bankingsystem.entity.Loan;
import com.bankingsystem.repository.LoanPaymentRepository;
import com.bankingsystem.repository.LoanRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanOverdueScheduler {

    private final LoanPaymentRepository loanPaymentRepository;
    private final LoanRepository loanRepository;

    private static final BigDecimal DAILY_PENALTY_RATE = new BigDecimal("0.001"); // 0.1% per day

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void processOverduePayments() {
        LocalDate today = LocalDate.now();

        List<LoanPayment> overduePayments = loanPaymentRepository.findByStatusInAndDueDateBefore(
                List.of(LoanPaymentStatus.SCHEDULED, LoanPaymentStatus.PARTIAL, LoanPaymentStatus.OVERDUE),
                today
        );

        int count = 0;
        for (LoanPayment payment : overduePayments) {
            long overdueDays = ChronoUnit.DAYS.between(payment.getDueDate(), today);
            if (overdueDays <= 0) continue;

            BigDecimal unpaid = payment.getTotalAmount().subtract(payment.getPaidAmount());
            BigDecimal penalty = unpaid.multiply(DAILY_PENALTY_RATE)
                    .multiply(new BigDecimal(overdueDays))
                    .setScale(2, RoundingMode.HALF_UP);

            payment.setPenalty(penalty);
            payment.setStatus(LoanPaymentStatus.OVERDUE);
            loanPaymentRepository.save(payment);

            Loan loan = payment.getLoan();
            loan.setOverdueDays((int) overdueDays);
            if (loan.getStatus() == LoanStatus.ACTIVE) {
                loan.setStatus(LoanStatus.OVERDUE);
            }
            loanRepository.save(loan);

            count++;
        }

        if (count > 0) {
            log.info("{} ta kredit to'lovi OVERDUE qilindi", count);
        }
    }
}



