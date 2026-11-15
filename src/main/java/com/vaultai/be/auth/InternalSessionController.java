package com.vaultai.be.auth;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Called by the auth-server after a password change (protected by InternalApiKeyFilter). */
@Hidden
@Slf4j
@RestController
@RequestMapping("/internal/sessions")
@RequiredArgsConstructor
public class InternalSessionController {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public record RevokeSessionsRequest(@NotBlank String userId) {
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@Valid @RequestBody RevokeSessionsRequest request) {
        // the session principal name is the "sub" claim, i.e. the user id
        Map<String, ? extends Session> sessions = sessionRepository.findByPrincipalName(request.userId());
        sessions.keySet().forEach(sessionRepository::deleteById);
        log.info("Revoked {} sessions for user {}", sessions.size(), request.userId());
        return ResponseEntity.noContent().build();
    }
}