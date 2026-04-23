package com.bankingsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bankingsystem.entity.LoanApplication;
import com.bankingsystem.entity.LoanApplicationStatus;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LoanApplication> findByStatusOrderByCreatedAtDesc(LoanApplicationStatus status);
}


