package com.vaultai.be.auth;

import java.util.List;
import java.util.UUID;

public record MeResponse(UUID id, String email, List<String> roles) {
}