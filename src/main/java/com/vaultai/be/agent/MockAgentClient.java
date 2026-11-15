package com.vaultai.be.agent;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Fake agent with simple keyword rules, for development and as a demo fallback.
 * Examples:
 * - "What is my balance?"
 * - "Show my last transactions"
 * - "Transfer 150 to RO49AAAA1B31007593840000"
 * - "Transfer 50 from account <uuid> to RO49AAAA1B31007593840000"
 *   (with someone else's account id, to show that the backend rejects it)
 */
@Slf4j
@Component
@Profile("mock")
public class MockAgentClient implements AgentClient {

    private static final Pattern TRANSFER_AMOUNT = Pattern.compile(
            "(?:transfer|send|pay)\\s+(\\d+(?:[.,]\\d{1,2})?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IBAN = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z0-9]{11,30}$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final String HELP = """
            I can show your balance, list your recent transactions or prepare a transfer. Try:
            "What is my balance?", "Show my last transactions" or "Transfer 150 to RO49AAAA1B31007593840000".""";

    @Override
    public AgentResponse send(AgentRequest request) {
        log.debug("Mock agent received: {}", request.message());

        if (request.context().accounts().isEmpty()) {
            return new AgentResponse("You don't have any accounts yet.", null);
        }

        String message = request.message();
        String text = message.toLowerCase(Locale.ROOT);
        String accountId = explicitAccountId(message).orElseGet(() -> mainAccountId(request));

        if (containsAny(text, "transfer", "send", "pay")) {
            return transfer(message, accountId);
        }
        if (containsAny(text, "transaction", "history", "spent", "payments")) {
            return new AgentResponse("Here are your most recent transactions.",
                    new ProposedAction("GET_TRANSACTIONS", Map.of("accountId", accountId, "limit", 10)));
        }
        if (containsAny(text, "balance", "how much")) {
            return new AgentResponse("Here is the current balance of your account.",
                    new ProposedAction("GET_BALANCE", Map.of("accountId", accountId)));
        }
        return new AgentResponse(HELP, null);
    }

    private AgentResponse transfer(String message, String accountId) {
        Matcher amountMatcher = TRANSFER_AMOUNT.matcher(message);
        Optional<String> iban = findIban(message);

        if (!amountMatcher.find() || iban.isEmpty()) {
            return new AgentResponse(
                    "To make a transfer, tell me the amount and the IBAN, for example: "
                            + "\"Transfer 150 to RO49AAAA1B31007593840000\".",
                    null);
        }

        String amount = amountMatcher.group(1).replace(',', '.');
        return new AgentResponse(
                "I've prepared a transfer of %s to %s. Please review the details and confirm it."
                        .formatted(amount, iban.get()),
                new ProposedAction("TRANSFER", Map.of(
                        "fromAccountId", accountId,
                        "toIban", iban.get(),
                        "amount", amount,
                        "description", "Transfer requested via Vault AI")));
    }

    private Optional<String> findIban(String message) {
        for (String word : message.split("\\s+")) {
            String candidate = word.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            if (IBAN.matcher(candidate).matches()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Optional<String> explicitAccountId(String message) {
        Matcher matcher = UUID_PATTERN.matcher(message);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }

    private String mainAccountId(AgentRequest request) {
        return request.context().accounts().stream()
                .filter(account -> "CURRENT".equals(account.type()))
                .findFirst()
                .orElse(request.context().accounts().getFirst())
                .id();
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}