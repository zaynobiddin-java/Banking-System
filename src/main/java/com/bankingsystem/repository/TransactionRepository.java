package com.bankingsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(Long fromId, Long toId);

    @Query("""
            select t
            from Transaction t
            where (t.fromAccount.id = :accountId or t.toAccount.id = :accountId)
              and (:fromDateTime is null or t.createdAt >= :fromDateTime)
              and (:toDateTimeExclusive is null or t.createdAt < :toDateTimeExclusive)
              and (:minAmount is null or t.amount >= :minAmount)
              and (:maxAmount is null or t.amount <= :maxAmount)
              and (:type is null or t.type = :type)
            order by t.createdAt desc
            """)
    List<Transaction> findAccountHistoryWithFilters(
            @Param("accountId") Long accountId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("type") TransactionType type
    );
}


