package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.ai.Assistant;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AssistantTest {

    @Inject
    Assistant assistant;

    @Test
    public void testNeedInfo() {
        System.out.println(assistant.ask("What is the capital?"));
    }

    @Test
    public void testAllInfo() {
        System.out.println(assistant.ask("What is the capital of Japan?"));
    }

}
