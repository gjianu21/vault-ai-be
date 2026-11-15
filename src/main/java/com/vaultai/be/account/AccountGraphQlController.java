package com.vaultai.be.account;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;

import com.vaultai.be.auth.CurrentUser;

import lombok.RequiredArgsConstructor;

/** GraphQL queries from schema.graphqls. Read-only. */
@Controller
@RequiredArgsConstructor
public class AccountGraphQlController {

    private final AccountService accountService;

    @QueryMapping
    public List<AccountDto> accounts(@AuthenticationPrincipal OidcUser user) {
        return accountService.getAccounts(CurrentUser.id(user));
    }

    @QueryMapping
    public AccountDto account(@Argument UUID id, @AuthenticationPrincipal OidcUser user) {
        return accountService.getAccount(CurrentUser.id(user), id);
    }

    @QueryMapping
    public List<TransactionDto> transactions(@Argument UUID accountId, @Argument Integer limit,
                                             @AuthenticationPrincipal OidcUser user) {
        return accountService.getTransactions(CurrentUser.id(user), accountId, limit);
    }

    /** The nested field Account.transactions. */
    @SchemaMapping(typeName = "Account", field = "transactions")
    public List<TransactionDto> accountTransactions(AccountDto account, @Argument Integer limit,
                                                    @AuthenticationPrincipal OidcUser user) {
        return accountService.getTransactions(CurrentUser.id(user), account.id(), limit);
    }
}