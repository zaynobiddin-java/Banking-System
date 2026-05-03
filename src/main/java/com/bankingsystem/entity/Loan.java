package com.bankingsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type")
    private LoanType loanType;

    private BigDecimal amount;

    @Column(name = "interest_rate")
    private BigDecimal interestRate;

    @Column(name = "duration_month")
    private Integer durationMonth;

    @Column(name = "monthly_payment")
    private BigDecimal monthlyPayment;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "remaining_amount")
    private BigDecimal remainingAmount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Column(name = "overdue_days")
    @Builder.Default
    private Integer overdueDays = 0;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "loan")
    private List<LoanPayment> payments;
}



