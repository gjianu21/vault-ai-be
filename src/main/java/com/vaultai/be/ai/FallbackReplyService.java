package com.vaultai.be.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The reply when the AI does not answer. The technical reason is logged and saved, never shown.
 * When the JARVIS data is ready, this will render a template briefing from the customer view.
 */
@Component
public class FallbackReplyService {

    public String reply(String userMessage, String context) {
        if (!StringUtils.hasText(context)) {
            return "I can't reach my AI engine right now, so I can't answer that in natural language. "
                    + "Please try again in a moment.";
        }
        return "I can't reach my AI engine right now. Here is the information from the bank's systems:\n\n"
                + context;
    }
}