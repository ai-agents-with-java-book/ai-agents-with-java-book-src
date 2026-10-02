package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class TicketResourceTest {

    // -------------------------------------------------------------------------
    // Happy path — the full workflow runs against the real (demo) LLM endpoint.
    // We cannot assert the exact text, but we can assert the call succeeds and
    // returns a non-empty plain-text body.
    // -------------------------------------------------------------------------

    // My seat was broken during the flight.

    @Test
    void shouldReturnAIResponseForValidRequest() {
        String response = given()
                .contentType("application/json")
                .body("""
                        {
                          "user":      "John Doe",
                          "email":     "john@example.com",
                          "bookingId": 42,
                          "message":   "The experience was great."
                        }
                        """)
        .when()
                .post("/ticket/storetest")
        .then()
                .extract().response().asString();

        System.out.println("***+++ " + response);
    }

    @Test
    void shouldReturnAIResponseForValidRequestAsync() {
        String response = given()
            .contentType("application/json")
            .body("""
                        {
                          "user":      "John Doe",
                          "email":     "john@example.com",
                          "bookingId": 42,
                          "message":   "My seat was broken during the flight."
                        }
                        """)
            .when()
            .post("/ticket/store")
            .then()
            .extract().response().asString();

        /**
         * {"request":{"user":"V","email":"v@example.com","bookingId":1,"message":"my seat was broken during the flight."},"response":{"draft":"Thank you very much for your kind words! We appreciate your feedback and are here to assist you further. If you have any questions or need assistance regarding your trip, please feel free to ask. Our team is ready to help with any information about your travel plans, policies, or options available to you. We want to ensure you have a smooth and enjoyable experience with us!","sentiment":{"reason":"The user reported an issue with their seat being broken during the flight, indicating dissatisfaction and a negative experience.","result":"NEGATIVE"}}}
         */

        System.out.println("***+++ " + response);
    }
    
}
