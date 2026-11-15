package com.vaultai.be.action;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.vaultai.be.account.Account;
import com.vaultai.be.account.AccountService;
import com.vaultai.be.agent.ProposedAction;
import com.vaultai.be.common.NotFoundException;
import com.vaultai.be.config.AppProperties;

import lombok.RequiredArgsConstructor;

/**
 * Turns an untrusted ProposedAction from the agent into a ValidatedAction.
 * The owner always comes from the session, never from the agent.
 */
@Component
@RequiredArgsConstructor
public class ActionValidator {

    private static final Pattern IBAN = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z0-9]{11,30}$");
    private static final int DEFAULT_TRANSACTIONS_LIMIT = 10;
    private static final int MAX_DESCRIPTION_LENGTH = 140;
    private static final String DEFAULT_DESCRIPTION = "Transfer via Vault AI";

    private final AccountService accountService;
    private final AppProperties properties;

    public ValidatedAction validate(UUID ownerId, ProposedAction proposal) {
        if (proposal == null) {
            throw new ActionRejectedException("Empty action");
        }

        ActionType type = ActionType.parse(proposal.type())
                .orElseThrow(() -> new ActionRejectedException("Unsupported action"));
        Map<String, Object> params = proposal.parameters() == null ? Map.of() : proposal.parameters();

        return switch (type) {
            case GET_BALANCE -> new ValidatedAction.GetBalance(
                    ownedAccount(ownerId, params, "accountId").getId());
            case GET_TRANSACTIONS -> new ValidatedAction.GetTransactions(
                    ownedAccount(ownerId, params, "accountId").getId(), limit(params));
            case TRANSFER -> validateTransfer(ownerId, params);
        };
    }

    /** Checked again at confirmation: the balance or the limits may have changed meanwhile. */
    public void revalidateTransfer(UUID ownerId, ValidatedAction.Transfer transfer) {
        Account from = ownedAccount(ownerId, transfer.fromAccountId());
        checkLimits(ownerId, from, transfer.amount());
    }

    private ValidatedAction.Transfer validateTransfer(UUID ownerId, Map<String, Object> params) {
        Account from = ownedAccount(ownerId, params, "fromAccountId");

        String toIban = normalizeIban(requiredString(params, "toIban"));
        if (!IBAN.matcher(toIban).matches()) {
            throw new ActionRejectedException("Invalid IBAN");
        }
        if (toIban.equals(from.getIban())) {
            throw new ActionRejectedException("The source and destination accounts are the same");
        }

        BigDecimal amount = amount(params);
        checkLimits(ownerId, from, amount);

        return new ValidatedAction.Transfer(from.getId(), toIban, amount, from.getCurrency(), description(params));
    }

    private void checkLimits(UUID ownerId, Account from, BigDecimal amount) {
        AppProperties.Limits limits = properties.limits();

        if (amount.compareTo(limits.maxTransferAmount()) > 0) {
            throw new ActionRejectedException("The amount exceeds the limit of %s %s per transfer"
                    .formatted(limits.maxTransferAmount().toPlainString(), from.getCurrency()));
        }
        if (amount.compareTo(from.getBalance()) > 0) {
            throw new ActionRejectedException("Insufficient funds");
        }

        Instant startOfDay = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant();
        BigDecimal spentToday = accountService.outgoingAmountSince(ownerId, startOfDay);
        if (spentToday.add(amount).compareTo(limits.dailyTransferLimit()) > 0) {
            throw new ActionRejectedException("The amount exceeds your daily limit of %s %s"
                    .formatted(limits.dailyTransferLimit().toPlainString(), from.getCurrency()));
        }
    }

    private Account ownedAccount(UUID ownerId, Map<String, Object> params, String key) {
        String raw = requiredString(params, key);
        UUID accountId;
        try {
            accountId = UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ActionRejectedException("Invalid account id");
        }
        return ownedAccount(ownerId, accountId);
    }

    private Account ownedAccount(UUID ownerId, UUID accountId) {
        try {
            return accountService.requireOwnedAccount(ownerId, accountId);
        } catch (NotFoundException ex) {
            // same message whether the account does not exist or belongs to someone else
            throw new ActionRejectedException("Account not found");
        }
    }

    private BigDecimal amount(Map<String, Object> params) {
        String raw = requiredString(params, "amount");
        BigDecimal amount;
        try {
            amount = new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            throw new ActionRejectedException("Invalid amount");
        }
        if (amount.signum() <= 0) {
            throw new ActionRejectedException("The amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new ActionRejectedException("The amount can have at most 2 decimals");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private int limit(Map<String, Object> params) {
        Object raw = params.get("limit");
        int max = Math.min(50, properties.limits().maxTransactionsPerQuery());
        if (raw == null) {
            return Math.min(DEFAULT_TRANSACTIONS_LIMIT, max);
        }
        try {
            int value = Integer.parseInt(raw.toString().trim());
            return Math.max(1, Math.min(value, max));
        } catch (NumberFormatException ex) {
            throw new ActionRejectedException("Invalid limit");
        }
    }

    private String description(Map<String, Object> params) {
        Object raw = params.get("description");
        if (raw == null || raw.toString().isBlank()) {
            return DEFAULT_DESCRIPTION;
        }
        String clean = raw.toString().replaceAll("\\p{Cntrl}", " ").trim();
        return clean.length() > MAX_DESCRIPTION_LENGTH ? clean.substring(0, MAX_DESCRIPTION_LENGTH) : clean;
    }

    private static String requiredString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ActionRejectedException("Missing parameter: " + key);
        }
        return value.toString();
    }

    private static String normalizeIban(String iban) {
        return iban.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}