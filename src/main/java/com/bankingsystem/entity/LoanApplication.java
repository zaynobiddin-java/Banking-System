package com.bankingsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    private BigDecimal amount;

    @Column(name = "duration_month")
    private Integer durationMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type")
    private LoanType loanType;

    @Column(name = "monthly_income")
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    private LoanApplicationStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @ManyToOne
    @JoinColumn(name = "disbursement_account_id")
    private Account disbursementAccount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}


