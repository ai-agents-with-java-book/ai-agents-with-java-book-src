package org.aiagents.chatmemory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;

public class UppercaseOutputGuardrail implements OutputGuardrail {

//    @Override
//    public OutputGuardrailResult validate(AiMessage aiMessage) {
//        String message = aiMessage.text();
//        boolean isAllUppercase = message.chars()
//                .filter(Character::isLetter)
//                .allMatch(Character::isUpperCase);
//
//        if (isAllUppercase) {
//            return success();
//        } else {
//            return fatal("The output must be in uppercase.");
////            return successWith(message.toUpperCase());
//        }
//    }

    @Override
    public OutputGuardrailResult validate(OutputGuardrailRequest request) {
        String message = request.responseFromLLM().aiMessage().text();
        boolean isAllUppercase = message.chars()
                .filter(Character::isLetter)
                .allMatch(Character::isUpperCase);

        if (isAllUppercase) {
            return success();
        }

        String userMessage = request.requestParams().userMessageTemplate();
        return reprompt("The output must be in uppercase.", userMessage + " Please reply all in uppercase.");
    }
}