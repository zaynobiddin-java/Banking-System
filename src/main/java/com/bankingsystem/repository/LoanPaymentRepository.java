package com.bankingsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bankingsystem.entity.LoanPayment;
import com.bankingsystem.entity.LoanPaymentStatus;

import java.time.LocalDate;
import java.util.List;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {

    List<LoanPayment> findByLoanIdOrderByPaymentNumberAsc(Long loanId);

    List<LoanPayment> findByStatusInAndDueDateBefore(List<LoanPaymentStatus> statuses, LocalDate date);
}


