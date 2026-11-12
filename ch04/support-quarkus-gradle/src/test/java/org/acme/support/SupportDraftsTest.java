package org.acme.support;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SupportDraftsTest {
    @Test
    void rejectsBlankInputBeforeCallingTheModel() {
        var calls = new AtomicInteger();
        var drafts = new SupportDrafts(request -> {
            calls.incrementAndGet();
            return "Unexpected call";
        });
        assertThrows(IllegalArgumentException.class,
            () -> drafts.prepareSummary("  "));
        assertEquals(0, calls.get());
    }

    @Test
    void passesTheRequestAndReturnsTheSummary() {
        var drafts = new SupportDrafts(request -> {
            assertEquals("Where is ORD-1042?", request);
            return "The customer asks about ORD-1042.";
        });
        assertEquals("The customer asks about ORD-1042.",
            drafts.prepareSummary("Where is ORD-1042?"));
    }

    @Test
    void preservesFailureCause() {
        var cause = new IllegalStateException("Connection refused");
        var drafts = new SupportDrafts(request -> { throw cause; });
        var failure = assertThrows(SummaryUnavailableException.class,
            () -> drafts.prepareSummary("Where is ORD-1042?"));
        assertSame(cause, failure.getCause());
    }
}
