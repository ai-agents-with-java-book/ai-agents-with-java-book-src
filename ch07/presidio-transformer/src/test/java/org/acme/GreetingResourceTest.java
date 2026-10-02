package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class GreetingResourceTest {
    @Inject
    PresidioService presidioService;

    @Test
    void testAnonymizesPersonalData() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(containsString("dual-monitor setups"))
             .body(containsString("ANONYMIZED"))
             .body(not(containsString("Sarah Mitchell")))
             .body(not(containsString("sarah.mitchell@example.com")))
             .body(not(containsString("(415) 829-3476")));
    }

    @Test
    void testServiceDoesNotLogDocumentText() throws Exception {
        String document;
        try (var stream = getClass().getResourceAsStream("/email.txt")) {
            document = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        var messages = new StringBuffer();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.append(record.getMessage()).append('\n');
            }
            @Override
            public void flush() { }
            @Override
            public void close() { }
        };
        Logger logger = Logger.getLogger(PresidioService.class.getName());
        var stdout = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        String anonymized;
        logger.addHandler(handler);
        try (var capturedOut = new PrintStream(stdout, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            anonymized = presidioService.process(document);
        } finally {
            System.setOut(originalOut);
            logger.removeHandler(handler);
        }

        assertTrue(anonymized.contains("ANONYMIZED"));
        assertTrue(anonymized.contains("dual-monitor setups"));
        String output = messages + stdout.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains("Sarah Mitchell"));
        assertFalse(output.contains("sarah.mitchell@example.com"));
        assertFalse(output.contains("(415) 829-3476"));
        assertFalse(output.contains("dual-monitor setups"));
    }

}