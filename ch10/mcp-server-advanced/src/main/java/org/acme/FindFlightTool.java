package org.acme;

import io.quarkiverse.mcp.server.Elicitation;
import io.quarkiverse.mcp.server.ElicitationRequest;
import io.quarkiverse.mcp.server.ElicitationResponse;
import io.quarkiverse.mcp.server.Tool;

public class FindFlightTool {


    @Tool(description = "Find Flights")
    String findFlights(Elicitation elicitation) {
        if (elicitation.isSupported()) {

            var schema = elicitation.requestBuilder()
                    .setMessage("Please fill out the flight information:")
                    .addSchemaProperty("from",
                            new ElicitationRequest.StringSchema(
                                    "From",
                                    "Origin city",
                                    20,
                                    3,
                                    null,
                                    true))
                    .addSchemaProperty("to",
                            new ElicitationRequest.StringSchema(
                                    "To",
                                    "Destination city",
                                    20,
                                    3,
                                    null,
                                    true))
                    .addSchemaProperty("departureDate",
                            new ElicitationRequest.StringSchema("Departure",
                                    "Departure Date",
                                    10,
                                    10,
                                    ElicitationRequest.StringSchema.Format.DATE,
                                    true))
                    .build();


            ElicitationResponse response = schema.sendAndAwait();

            if (response.actionAccepted()) {
                String from = response.content().getString("from");
                String to = response.content().getString("to");
                String departureDate = response.content().getString("departureDate");

                // Logic to search the flight

                String flightInfo = "From: %s To: %s Date: %s".formatted(from, to, departureDate);
                System.out.println(flightInfo);

                return flightInfo;

            } else {
                return "Couldn't get all trip information";
            }

        } else {
            return "Elicitation not supported by this client";
        }
    }
}
