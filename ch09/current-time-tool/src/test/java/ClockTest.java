import org.acme.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClockTest {

    @Test
    void omittedUserUsesTheSystemTimeZone() {
        assertCurrentTimeInZone(null, ZoneId.systemDefault());
    }

    @Test
    void suppliedUserUsesTheExampleUserTimeZone() {
        assertCurrentTimeInZone("1234", ZoneId.of("CET"));
    }

    private void assertCurrentTimeInZone(String userId, ZoneId expectedZone) {
        Instant before = Instant.now();
        ZonedDateTime result = ZonedDateTime.parse(new Clock().getNow(userId));
        Instant after = Instant.now();

        assertEquals(expectedZone, result.getZone());
        assertFalse(result.toInstant().isBefore(before));
        assertFalse(result.toInstant().isAfter(after));
    }
}
