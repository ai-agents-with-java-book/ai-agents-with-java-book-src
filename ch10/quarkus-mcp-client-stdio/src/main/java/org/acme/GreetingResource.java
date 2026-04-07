package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/exchange")
public class GreetingResource {

    @Inject
    ExchangeRateAssistant exchangeRateAssistant;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return exchangeRateAssistant.chat("10 euros in dollars?");
    }
}
