package com.vaultai.be.action;

/** The proposed action failed validation. The message is safe to show to the user. */
public class ActionRejectedException extends RuntimeException {

    public ActionRejectedException(String reason) {
        super(reason);
    }
}