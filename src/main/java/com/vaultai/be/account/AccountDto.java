package com.vaultai.be.account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDto(
        UUID id,
        String iban,
        String name,
        AccountType type,
        String currency,
        BigDecimal balance) {

    public static AccountDto from(Account account) {
        return new AccountDto(account.getId(), account.getIban(), account.getName(),
                account.getType(), account.getCurrency(), account.getBalance());
    }
}