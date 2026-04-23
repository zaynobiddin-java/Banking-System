package com.bankingsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bankingsystem.entity.LoanStatus;
import com.bankingsystem.entity.Loan;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Loan> findByStatus(LoanStatus status);
}



