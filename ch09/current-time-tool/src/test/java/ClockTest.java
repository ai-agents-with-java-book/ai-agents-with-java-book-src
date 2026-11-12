import org.acme.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClockTest {

    @Test
    void omittedTimeZoneUsesTheSystemTimeZone() {
        assertCurrentTimeInZone(null, ZoneId.systemDefault());
    }

    @Test
    void suppliedTimeZoneUsesTheRequestedTimeZone() {
        assertCurrentTimeInZone("Europe/Berlin", ZoneId.of("Europe/Berlin"));
    }

    private void assertCurrentTimeInZone(String timeZone, ZoneId expectedZone) {
        Instant before = Instant.now();
        ZonedDateTime result = ZonedDateTime.parse(new Clock().getNow(timeZone));
        Instant after = Instant.now();

        assertEquals(expectedZone, result.getZone());
        assertFalse(result.toInstant().isBefore(before));
        assertFalse(result.toInstant().isAfter(after));
    }
}
