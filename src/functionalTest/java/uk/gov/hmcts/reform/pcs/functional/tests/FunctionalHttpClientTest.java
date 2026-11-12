package uk.gov.hmcts.reform.pcs.functional.tests;

import com.sun.net.httpserver.HttpServer;
import io.restassured.http.ContentType;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.rest.SerenityRest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.equalTo;

@Tag("Functional")
@ExtendWith(SerenityJUnit5Extension.class)
class FunctionalHttpClientTest {

    @Test
    void sendsAGetWithQueryParametersUsingTheFunctionalTestClient() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/eligibility", exchange -> {
            String query = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
            boolean matches = "GET".equals(exchange.getRequestMethod()) && "postcode=W3 7RX".equals(query);
            byte[] body = "{\"status\":\"ELIGIBLE\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(matches ? 200 : 400, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            SerenityRest.given()
                .baseUri("http://127.0.0.1:" + server.getAddress().getPort())
                .contentType(ContentType.JSON)
                .queryParam("postcode", "W3 7RX")
                .when().get("/eligibility")
                .then().statusCode(200).body("status", equalTo("ELIGIBLE"));
        } finally {
            server.stop(0);
        }
    }
}
