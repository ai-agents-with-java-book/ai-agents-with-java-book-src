package ch15.cascade;

import ch15.Models;

/**
 * Answers a support request with the confidence cascade: a small model first, a large model
 * when its answer is not good enough, and a human when neither is. Requires OPENAI_API_KEY.
 */
public class CascadeMain {

    public static void main(String[] args) {
        SupportCascade supportCascade = CascadeExamples.supportCascade(Models.decisionModel());
        CascadeExamples.ask(supportCascade);
    }
}
