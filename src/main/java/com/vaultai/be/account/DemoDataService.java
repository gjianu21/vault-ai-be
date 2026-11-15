package com.vaultai.be.account;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaultai.be.config.AppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates two demo accounts with some history at the user's first login.
 * The IBANs look real but are not valid bank accounts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String IBAN_PREFIX = "RO49VAUL";

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final AppProperties properties;

    @Transactional
    public void ensureDemoData(UUID ownerId) {
        if (accountRepository.existsByOwnerId(ownerId)) {
            return;
        }

        String currency = properties.demoData().currency();
        Instant now = Instant.now();

        Account current = accountRepository.save(newAccount(ownerId, "Current account", AccountType.CURRENT, currency));
        addTransaction(current, TransactionType.CREDIT, "8500.00", "Monthly salary", "Acme Software SRL", now, 32);
        addTransaction(current, TransactionType.DEBIT, "2200.00", "Rent", "Home Rentals SRL", now, 30);
        addTransaction(current, TransactionType.DEBIT, "412.35", "Groceries", "Mega Market", now, 27);
        addTransaction(current, TransactionType.DEBIT, "189.90", "Electricity bill", "Power Supply SA", now, 21);
        addTransaction(current, TransactionType.DEBIT, "64.50", "Restaurant", "Bistro Central", now, 14);
        addTransaction(current, TransactionType.DEBIT, "1500.00", "Transfer to savings", "Vault AI Savings", now, 10);
        addTransaction(current, TransactionType.DEBIT, "236.80", "Fuel", "Fuel Station", now, 6);
        addTransaction(current, TransactionType.CREDIT, "8500.00", "Monthly salary", "Acme Software SRL", now, 2);

        Account savings = accountRepository.save(newAccount(ownerId, "Savings account", AccountType.SAVINGS, currency));
        addTransaction(savings, TransactionType.CREDIT, "25000.00", "Opening deposit", null, now, 90);
        addTransaction(savings, TransactionType.CREDIT, "1500.00", "Monthly savings", "Vault AI Current", now, 10);

        log.info("Demo accounts created for user {}", ownerId);
    }

    private Account newAccount(UUID ownerId, String name, AccountType type, String currency) {
        return Account.builder()
                .ownerId(ownerId)
                .iban(generateIban())
                .name(name)
                .type(type)
                .currency(currency)
                .balance(BigDecimal.ZERO.setScale(2))
                .createdAt(Instant.now())
                .build();
    }

    private void addTransaction(Account account, TransactionType type, String amount, String description,
                                String counterparty, Instant now, int daysAgo) {
        BigDecimal value = new BigDecimal(amount);
        if (type == TransactionType.CREDIT) {
            account.credit(value);
        } else {
            account.debit(value);
        }

        transactionRepository.save(AccountTransaction.builder()
                .account(account)
                .type(type)
                .amount(value)
                .currency(account.getCurrency())
                .description(description)
                .counterpartyName(counterparty)
                .bookedAt(now.minus(Duration.ofDays(daysAgo)))
                .balanceAfter(account.getBalance())
                .build());
    }

    private String generateIban() {
        String iban;
        do {
            StringBuilder digits = new StringBuilder(IBAN_PREFIX);
            for (int i = 0; i < 16; i++) {
                digits.append(RANDOM.nextInt(10));
            }
            iban = digits.toString();
        } while (accountRepository.existsByIban(iban));
        return iban;
    }
}