package org.acme;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Clock {

    @Tool("""
            Gets the current date and time in the requested time zone.
            The current date and time is returned in ISO-8601 format
        """
    ) // <1>
    public String getNow(@P(name = "timeZone",
                           value = "The time zone ID, for example Europe/Berlin",
                           required = false) String timeZone) { // <2>

        ZoneId zone = timeZone == null
                ? ZoneId.systemDefault() : ZoneId.of(timeZone); // <3>
        ZonedDateTime now = ZonedDateTime.now(zone);
        return now.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
