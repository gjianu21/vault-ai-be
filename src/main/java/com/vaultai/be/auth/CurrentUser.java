package com.vaultai.be.auth;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The only place where the user id is read: always from the authenticated session,
 * never from a request body and never from the AI agent.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(OidcUser user) {
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        return UUID.fromString(user.getSubject());
    }

    public static UUID id(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
            return id(user);
        }
        throw new AuthenticationCredentialsNotFoundException("Not authenticated");
    }
}