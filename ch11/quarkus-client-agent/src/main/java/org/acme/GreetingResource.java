package org.acme;


import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/greeting")
public class GreetingResource {

    @Inject
    HelloWorldAgentSec helloWorldAgent;

    @GET
    public String hello() {

        System.out.println(helloWorldAgent.greeting("hi"));

        return "hello";

    }

}
