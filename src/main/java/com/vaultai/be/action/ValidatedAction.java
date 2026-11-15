package com.vaultai.be.action;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * An action that passed validation: typed parameters, owned accounts, limits checked.
 * Sealed: the switch in ActionService must handle every case, or it does not compile.
 */
public sealed interface ValidatedAction {

    ActionType type();

    record GetBalance(UUID accountId) implements ValidatedAction {
        @Override
        public ActionType type() {
            return ActionType.GET_BALANCE;
        }
    }

    record GetTransactions(UUID accountId, int limit) implements ValidatedAction {
        @Override
        public ActionType type() {
            return ActionType.GET_TRANSACTIONS;
        }
    }

    record Transfer(UUID fromAccountId, String toIban, BigDecimal amount, String currency, String description)
            implements ValidatedAction {
        @Override
        public ActionType type() {
            return ActionType.TRANSFER;
        }
    }
}