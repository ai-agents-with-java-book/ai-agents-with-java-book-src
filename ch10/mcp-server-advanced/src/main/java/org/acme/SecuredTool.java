package org.acme;

import io.quarkiverse.mcp.server.Tool;
import io.quarkus.security.Authenticated;

import java.util.Set;

public class SecuredTool {

    public record Customer(String name){}

    @Tool(description = "Gets customers data")
    @Authenticated
    Set<Customer> findCustomers() {

        return Set.of(new Customer("Alex"),
                new Customer("Ada"),
                new Customer("Aixa"));
    }

}
