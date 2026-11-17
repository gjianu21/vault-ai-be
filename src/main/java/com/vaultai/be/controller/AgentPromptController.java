package com.vaultai.be.controller;

import java.util.List;
import java.util.UUID;

import com.vaultai.be.dto.response.AgentPromptDetailDto;
import com.vaultai.be.dto.response.AgentPromptSummaryDto;
import com.vaultai.be.dto.response.AgentResult;
import com.vaultai.be.service.AgentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vaultai.be.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Agent prompts")
@RestController
@RequestMapping("/api/agent/prompts")
@RequiredArgsConstructor
public class AgentPromptController {

    private final AgentService agentService;

    @Operation(summary = "The user's prompts: last 50, or all prompts of one conversation")
    @GetMapping
    public List<AgentPromptSummaryDto> list(@RequestParam(required = false) UUID conversationId,
                                            @AuthenticationPrincipal OidcUser user) {
        return agentService.getPrompts(CurrentUser.id(user), conversationId);
    }

    @Operation(summary = "One prompt with the full text sent, the raw output and the reply")
    @GetMapping("/{promptId}")
    public AgentPromptDetailDto get(@PathVariable UUID promptId, @AuthenticationPrincipal OidcUser user) {
        return agentService.getPrompt(CurrentUser.id(user), promptId);
    }

    @Operation(summary = "Run a saved prompt again (saved as a new prompt)")
    @PostMapping("/{promptId}/replay")
    public AgentResult replay(@PathVariable UUID promptId, @AuthenticationPrincipal OidcUser user) {
        return agentService.replay(CurrentUser.id(user), promptId);
    }
}