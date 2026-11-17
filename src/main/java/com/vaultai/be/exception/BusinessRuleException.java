package com.vaultai.be.exception;

/** A rule of the domain was broken (insufficient funds, action no longer pending, ...). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}