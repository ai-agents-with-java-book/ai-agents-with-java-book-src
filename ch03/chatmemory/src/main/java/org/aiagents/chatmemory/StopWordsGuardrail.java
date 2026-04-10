package org.aiagents.chatmemory;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.List;
import java.util.stream.Stream;

public class StopWordsGuardrail implements InputGuardrail {

    public final List<String> stopWords;

    public StopWordsGuardrail(String... stopWords) {
        this.stopWords = Stream.of(stopWords).map(String::toLowerCase).toList();
    }

    public InputGuardrailResult validate(UserMessage userMessage) {
        for (String stopWord : stopWords) {
            if (userMessage.hasSingleText() && userMessage.singleText().toLowerCase().contains(stopWord)) {
                return failure("Input contains prohibited word: " + stopWord);
            }
        }
        return success();
    }
}
