package org.aiagents.chatmemory;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.List;
import java.util.stream.Stream;

public class AnonymizerGuardrail implements InputGuardrail {

    public final String[] forbiddenWords;

    public AnonymizerGuardrail(String... forbiddenWords) {
        this.forbiddenWords = forbiddenWords;
    }

    public InputGuardrailResult validate(UserMessage userMessage) {
        String originalMessage = userMessage.singleText();
        String rewritttenMessage = originalMessage;
        for (String forbiddenWord : forbiddenWords) {
            rewritttenMessage = rewritttenMessage.replace(forbiddenWord, "***");
        }
        return originalMessage.equals(rewritttenMessage) ? success() : successWith(rewritttenMessage);
    }
}
