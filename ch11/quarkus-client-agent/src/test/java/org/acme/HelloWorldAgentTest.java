package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class HelloWorldAgentTest {

    @Inject
    HelloWorldAgent helloWorldAgent;

    @Test
    public void test() {
        assertEquals("HI*", helloWorldAgent.greeting("hi"));
    }

}
