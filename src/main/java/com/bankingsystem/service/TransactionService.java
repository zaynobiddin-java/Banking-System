package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bankingsystem.dto.CardTransferRequest;
import com.bankingsystem.dto.DepositRequest;
import com.bankingsystem.dto.PaymentRequest;
import com.bankingsystem.dto.TransactionResponse;
import com.bankingsystem.dto.WithdrawRequest;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.AccountStatus;
import com.bankingsystem.entity.Card;
import com.bankingsystem.entity.CardStatus;
import com.bankingsystem.entity.Transaction;
import com.bankingsystem.entity.TransactionStatus;
import com.bankingsystem.entity.TransactionType;
import com.bankingsystem.exception.BadRequestException;
import com.bankingsystem.exception.NotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.CardRepository;
import com.bankingsystem.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final AccessControlService accessControlService;

    private static final BigDecimal CARD_TRANSFER_FEE_RATE = new BigDecimal("0.01"); // 1%
    private static final BigDecimal WITHDRAWAL_FEE_RATE = new BigDecimal("0.005");   // 0.5%
    private static final BigDecimal MIN_CARD_TRANSFER_FEE = new BigDecimal("500");

    @Transactional
    public TransactionResponse cardTransfer(CardTransferRequest request) {
        validateAmount(request.getAmount());

        if (request.getFromCardNumber().equals(request.getToCardNumber())) {
            throw new BadRequestException("O'ziga o'zi o'tkazma qilib bo'lmaydi");
        }

        Card fromCard = cardRepository.findByCardNumber(request.getFromCardNumber())
                .orElseThrow(() -> new NotFoundException("Jo'natuvchi karta topilmadi"));
        Card toCard = cardRepository.findByCardNumber(request.getToCardNumber())
                .orElseThrow(() -> new NotFoundException("Qabul qiluvchi karta topilmadi"));

        accessControlService.requireCardAccess(fromCard);
        validateCardActive(fromCard, "Jo'natuvchi");
        validateCardActive(toCard, "Qabul qiluvchi");

        Account fromAccount = fromCard.getAccount();
        Account toAccount = toCard.getAccount();

        validateAccountActive(fromAccount, "Jo'natuvchi");
        validateAccountActive(toAccount, "Qabul qiluvchi");

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new BadRequestException("Hisoblar valyutasi mos kelmaydi");
        }

        BigDecimal fee = calculateCardTransferFee(fromAccount, toAccount, request.getAmount());
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (fromAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new BadRequestException("Hisobda yetarli mablag' yo'q");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDebit));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = saveTransaction(fromAccount, toAccount, request.getAmount(), fee, TransactionType.TRANSFER);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        validateAmount(request.getAmount());

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        accessControlService.requireAccountAccess(account);
        validateAccountActive(account, "Hisob");

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = saveTransaction(null, account, request.getAmount(), BigDecimal.ZERO, TransactionType.DEPOSIT);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        validateAmount(request.getAmount());

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        accessControlService.requireAccountAccess(account);
        validateAccountActive(account, "Hisob");

        BigDecimal fee = request.getAmount().multiply(WITHDRAWAL_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = request.getAmount().add(fee);

        if (account.getBalance().compareTo(totalDebit) < 0) {
            throw new BadRequestException("Hisobda yetarli mablag' yo'q");
        }

        account.setBalance(account.getBalance().subtract(totalDebit));
        accountRepository.save(account);

        Transaction transaction = saveTransaction(account, null, request.getAmount(), fee, TransactionType.WITHDRAWAL);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse payment(PaymentRequest request) {
        validateAmount(request.getAmount());

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new BadRequestException("O'ziga o'zi to'lov qilib bo'lmaydi");
        }

        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new NotFoundException("Jo'natuvchi hisob topilmadi"));
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new NotFoundException("Qabul qiluvchi hisob topilmadi"));

        accessControlService.requireAccountAccess(fromAccount);
        validateAccountActive(fromAccount, "Jo'natuvchi");
        validateAccountActive(toAccount, "Qabul qiluvchi");

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new BadRequestException("Hisoblar valyutasi mos kelmaydi");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Hisobda yetarli mablag' yo'q");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = saveTransaction(fromAccount, toAccount, request.getAmount(), BigDecimal.ZERO, TransactionType.PAYMENT);
        return toResponse(transaction);
    }

    public List<TransactionResponse> getAccountHistory(
            Long accountId,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            TransactionType type
    ) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        accessControlService.requireAccountAccess(account);
        validateHistoryFilters(fromDate, toDate, minAmount, maxAmount);

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTimeExclusive = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;

        return transactionRepository
                .findAccountHistoryWithFilters(accountId, fromDateTime, toDateTimeExclusive, minAmount, maxAmount, type)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BigDecimal calculateCardTransferFee(Account from, Account to, BigDecimal amount) {
        if (from.getOwnerType() == to.getOwnerType() && from.getOwnerId().equals(to.getOwnerId())) {
            return BigDecimal.ZERO;
        }

        BigDecimal fee = amount.multiply(CARD_TRANSFER_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        return fee.max(MIN_CARD_TRANSFER_FEE);
    }

    private Transaction saveTransaction(Account from, Account to, BigDecimal amount, BigDecimal fee, TransactionType type) {
        Transaction transaction = Transaction.builder()
                .fromAccount(from)
                .toAccount(to)
                .amount(amount)
                .fee(fee)
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
        return transactionRepository.save(transaction);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Summa 0 dan katta bo'lishi kerak");
        }
    }

    private void validateHistoryFilters(LocalDate fromDate, LocalDate toDate, BigDecimal minAmount, BigDecimal maxAmount) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("Boshlanish sanasi tugash sanasidan keyin bo'lishi mumkin emas");
        }

        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimal summa manfiy bo'lishi mumkin emas");
        }

        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Maksimal summa manfiy bo'lishi mumkin emas");
        }

        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BadRequestException("Minimal summa maksimal summadan katta bo'lishi mumkin emas");
        }
    }

    private void validateCardActive(Card card, String role) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new BadRequestException(role + " kartasi aktiv emas");
        }
    }

    private void validateAccountActive(Account account, String role) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException(role + " hisobi aktiv emas");
        }
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .fromAccountId(t.getFromAccount() != null ? t.getFromAccount().getId() : null)
                .toAccountId(t.getToAccount() != null ? t.getToAccount().getId() : null)
                .amount(t.getAmount())
                .fee(t.getFee())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt())
                .build();
    }
}


