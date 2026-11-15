package com.vaultai.be.action;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaultai.be.auth.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Tag(name = "Actions")
@RestController
@RequestMapping("/api/actions")
@RequiredArgsConstructor
public class ActionController {

    private final ActionService actionService;

    @Operation(summary = "Actions waiting for confirmation (e.g. after a page reload)")
    @GetMapping("/pending")
    public List<ActionResultDto> pending(@AuthenticationPrincipal OidcUser user) {
        return actionService.pendingActions(CurrentUser.id(user));
    }

    @Operation(summary = "Confirm a pending action. Idempotency-Key: a new random value (UUID) for each confirmation")
    @PostMapping("/{actionId}/confirm")
    public ActionResultDto confirm(@PathVariable UUID actionId,
                                   @RequestHeader("Idempotency-Key") @Size(min = 8, max = 100) String idempotencyKey,
                                   @AuthenticationPrincipal OidcUser user) {
        return actionService.confirm(CurrentUser.id(user), actionId, idempotencyKey);
    }

    @Operation(summary = "Cancel a pending action")
    @PostMapping("/{actionId}/reject")
    public ActionResultDto reject(@PathVariable UUID actionId, @AuthenticationPrincipal OidcUser user) {
        return actionService.reject(CurrentUser.id(user), actionId);
    }
}