package uk.gov.hmcts.reform.pcs.ccd.config;

import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Exercises the HTTP client on the same runtime classpath as preview data setup. */
public final class HighLevelDataSetupHttpClientCheck {

    private HighLevelDataSetupHttpClientCheck() {
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/user-role", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean matches = "PUT".equals(exchange.getRequestMethod())
                && "{\"role\":\"caseworker-pcs\"}".equals(body);
            exchange.sendResponseHeaders(matches ? 201 : 400, -1);
            exchange.close();
        });
        server.start();
        try {
            RestAssured.given()
                .baseUri("http://127.0.0.1:" + server.getAddress().getPort())
                .contentType(ContentType.JSON)
                .body(Map.of("role", "caseworker-pcs"))
                .when().put("/api/user-role")
                .then().statusCode(201);
        } finally {
            server.stop(0);
        }
    }
}
