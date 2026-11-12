package org.acme;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.github.resilience4j.core.functions.CheckedFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

public class Clock {
    
    static RetryConfig config = RetryConfig.custom()
                                            .maxAttempts(3)
                                            .waitDuration(Duration.ofMillis(1000))
                                            .retryExceptions(IOException.class)
                                            .build();

    static RetryRegistry registry = RetryRegistry.of(config);

    @Tool("""
            Gets the current date and time in the requested time zone.
            The current date and time is returned in ISO-8601 format
        """
    )
    public String getNow(@P(name = "timeZone",
                           value = "The time zone ID, for example Europe/Berlin",
                           required = false) String timeZone) {

        Retry retry = registry.retry("toolGetNow", config);
        CheckedFunction<String, String> retryableFunction = Retry.decorateCheckedFunction(retry, this::getCurrentDateAndTime);
        
        try {
            return retryableFunction.apply(timeZone);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    private String getCurrentDateAndTime(String timeZone) throws IOException {
        System.out.println("Getting current date and time for the time zone: " + timeZone);
        ZoneId zone = timeZone == null ? ZoneId.systemDefault() : ZoneId.of(timeZone);
        ZonedDateTime now = ZonedDateTime.now(zone);

        return now.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    
}
