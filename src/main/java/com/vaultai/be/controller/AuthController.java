package com.vaultai.be.controller;

import java.util.List;
import java.util.Map;

import com.vaultai.be.dto.response.MeResponse;
import com.vaultai.be.security.CurrentUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Operation(summary = "The logged-in user (401 if not logged in)")
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal OidcUser user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .toList();
        return new MeResponse(CurrentUser.id(user), user.getEmail(), roles);
    }

    @Operation(summary = "Makes sure the XSRF-TOKEN cookie exists and returns its value")
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }
}