package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class HelloWorldAgentTest {

    @Inject
    HelloWorldAgent helloWorldAgent;

    @Test
    public void test() {
        System.out.println(helloWorldAgent.greeting("hi"));
    }

}
