package com.vaultai.be.ai;

import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vaultai.be.exception.AgentUnavailableException;
import com.vaultai.be.model.enums.ReplySource;

/**
 * Fake executor for development, with canned answers to preview the UI.
 * Temporary: replaced by NoAiPromptExecutor when the JARVIS data and rules are in place.
 * "#fail" simulates a CLI error, "#timeout" a timeout.
 */
@Component
@Profile("mock")
public class MockPromptExecutor implements PromptExecutor {

    private static final String NEXT_MEETING = """
            Your next meeting is with Max Müller today at 10:00 (Annual portfolio review) [CRM-MTG-0001].

            Max Müller has been a customer for 18 years. Premium segment, moderate risk profile, prefers the branch channel [CRM-C001].

            Products (3):
            - Premium Current Account: balance EUR 12,400 [PRD-0001]
            - Fixed-Term Deposit 12M: EUR 50,000 at 2.8%, matures in 30 days [PRD-0002]
            - Gold Credit Card: EUR 1,250 used of a EUR 10,000 limit [PRD-0003]

            What needs your attention:
            - A card complaint was resolved 6 days ago (fee refunded and foreign payment settings corrected) [CMP-0001]
            - Fixed-Term Deposit 12M of EUR 50,000 matures in 30 days [PRD-0002]

            My recommendation:
            - Check that the customer is satisfied with how the complaint was resolved.
            - Discuss reinvestment options before the maturity date.

            Would you like me to prepare the talking points?""";

    private static final String TALKING_POINTS = """
            Talking points for your meeting with Max Müller:

            1. Open with the relationship: Max Müller has been with the bank for 18 years [CRM-C001].
            2. Check that the customer is satisfied with how the complaint was resolved. Context: the foreign transaction fee was refunded 6 days ago [CMP-0001].
            3. Discuss reinvestment options before the maturity date. Context: EUR 50,000 becomes available in 30 days [PRD-0002].
            4. Refer to the last contact (6 days ago, email): he was informed that the complaint was resolved [CRM-INT-0102].
            5. Close by agreeing on the next steps and a follow-up date.

            Would you like a summary of the key investment options?""";

    private static final String INVESTMENT_OPTIONS = """
            Key investment options for Max Müller (moderate risk profile) [CRM-C001], based on the EUR 50,000 that becomes available in 30 days [PRD-0002]:

            - Fixed-Term Deposit 24M: low risk, 3.1% p.a. guaranteed, from EUR 5,000. Capital-protected deposit with a fixed rate for 24 months [PRD-CAT-01]
            - Flexible Savings Account: low risk, 1.8% p.a. variable, from EUR 0. Daily access to the money [PRD-CAT-02]
            - Global Bond Fund: medium risk, 3-4% p.a. expected, from EUR 1,000. Diversified investment-grade bonds [PRD-CAT-03]
            - Balanced Managed Portfolio: medium risk, 4-5% p.a. expected, from EUR 25,000. Professionally managed mix of equities and bonds [PRD-CAT-04]

            Products above the customer's risk profile are not shown.
            Remember to complete the suitability assessment before making a recommendation.""";

    private static final String ATTENTION_LIST = """
            These customers need your attention this week:

            1. Jonas Becker [CRM-C005]
               - The Current Account is overdrawn by EUR 640 [PRD-0011]
               - The Standard Credit Card uses 92% of its limit (EUR 4,600 of EUR 5,000) [PRD-0012]
               Next meeting: in 6 days at 16:00 [CRM-MTG-0005]
               Recommended: Offer a budgeting conversation or a restructuring option.

            2. Lukas Weber [CRM-C003]
               - Open fees complaint for 12 days: unexpected processing fees on the business loan [CMP-0002]
               Next meeting: in 4 days at 11:30 [CRM-MTG-0003]
               Recommended: Give the customer an update on the complaint before discussing any new product.

            3. Sofia Rossi [CRM-C004]
               - Fixed-Term Deposit 24M of EUR 120,000 matures in 7 days [PRD-0009]
               No meeting this week: contact the customer.
               Recommended: Discuss reinvestment options before the maturity date.

            4. Max Müller [CRM-C001]
               - A card complaint was resolved 6 days ago [CMP-0001]
               - Fixed-Term Deposit 12M of EUR 50,000 matures in 30 days [PRD-0002]
               Next meeting: today at 10:00 [CRM-MTG-0001]
               Recommended: Check that the customer is satisfied with how the complaint was resolved.

            Ask me about any of them by name for a full briefing.""";

    private static final String HELP = """
            I can prepare you for your next customer meeting, tell you which customers need attention this week, or brief you on any customer by name.

            Your upcoming meetings:
            - today at 10:00: Max Müller, Annual portfolio review [CRM-MTG-0001]
            - in 4 days at 11:30: Lukas Weber, Business loan review [CRM-MTG-0003]
            - in 6 days at 16:00: Jonas Becker, Account review [CRM-MTG-0005]""";

    @Override
    public CliResult execute(String prompt) {
        int index = prompt.lastIndexOf(PromptBuilder.NEW_MESSAGE_HEADER);
        String message = index < 0 ? prompt : prompt.substring(index + PromptBuilder.NEW_MESSAGE_HEADER.length()).strip();
        String text = message.toLowerCase(Locale.ROOT);

        if (text.contains("#timeout")) {
            throw new AgentUnavailableException("Simulated timeout");
        }
        if (text.contains("#fail")) {
            return new CliResult(1, "", "Simulated CLI failure", 5);
        }

        return new CliResult(0, cannedReply(text, prompt), "", 5);
    }

    private static String cannedReply(String text, String prompt) {
        if (text.contains("talking point")) {
            return TALKING_POINTS;
        }
        if (text.contains("invest") || text.contains("options")) {
            return INVESTMENT_OPTIONS;
        }
        if (text.contains("attention") || text.contains("this week")) {
            return ATTENTION_LIST;
        }
        if (text.contains("next meeting") || text.contains("next customer") || text.contains("prepare me")) {
            return NEXT_MEETING;
        }
        if (text.matches("^(yes|yeah|sure|ok|okay|please|go ahead)\\b.*")) {
            // "yes" answers the question asked at the end of the previous reply (it is in the prompt's history)
            return prompt.contains("Would you like a summary of the key investment options?")
                    ? INVESTMENT_OPTIONS
                    : TALKING_POINTS;
        }
        return HELP;
    }

    @Override
    public ReplySource source() {
        return ReplySource.MOCK;
    }
}