package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

@QuarkusTest
class GreetingResourceTest {
    @Test
    void testAnonymizesPersonalData() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(containsString("dual-monitor setups"))
             .body(containsString("ANONYMIZED"))
             .body(not(containsString("Sarah Mitchell")))
             .body(not(containsString("sarah.mitchell@example.com")))
             .body(not(containsString("(415) 829-3476")));
    }

}