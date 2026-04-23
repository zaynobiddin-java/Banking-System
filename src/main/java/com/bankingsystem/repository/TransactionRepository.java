package com.bankingsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bankingsystem.entity.Transaction;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(Long fromId, Long toId);
}


