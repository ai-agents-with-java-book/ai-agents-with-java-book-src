package org.acme.support;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SummaryGraphTest {
    @Test
    void carriesTheSummaryToTheFinalState() throws Exception {
        var graph = SummaryGraph.build(request -> {
            assertEquals("Where is ORD-1042?", request);
            return "The customer asks about ORD-1042.";
        });
        var state = graph.invoke(Map.of("request", "Where is ORD-1042?"))
            .orElseThrow();
        assertEquals("completed", state.<String>value("status").orElseThrow());
        assertEquals("The customer asks about ORD-1042.",
            state.<String>value("response").orElseThrow());
    }

    @Test
    void invalidInputNeverReachesTheSummaryStep() throws Exception {
        var calls = new AtomicInteger();
        var graph = SummaryGraph.build(request -> {
            calls.incrementAndGet();
            return "Unexpected call";
        });
        var failure = assertThrows(RuntimeException.class,
            () -> graph.invoke(Map.of("request", "  ")));
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertInstanceOf(IllegalArgumentException.class, cause);
        assertEquals("Support request is required", cause.getMessage());
        assertEquals(0, calls.get());
    }
}
