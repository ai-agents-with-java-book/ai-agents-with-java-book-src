package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class GreetingResourceTest {
    @Test
    void publishesTheAgentCard() {
        given()
          .when().get("/.well-known/agent-card.json")
          .then()
             .statusCode(200)
             .body("name", is("Hello World HIL Agent"));
    }

}