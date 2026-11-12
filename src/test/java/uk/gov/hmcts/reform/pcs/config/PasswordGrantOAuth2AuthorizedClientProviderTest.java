package uk.gov.hmcts.reform.pcs.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordGrantOAuth2AuthorizedClientProviderTest {

    private HttpServer tokenServer;
    private OAuth2AuthorizedClientManager manager;
    private final List<Map<String, String>> requests = new ArrayList<>();
    private String tokenResponse;
    private int responseStatus;

    @BeforeEach
    void setUp() throws IOException {
        tokenResponse = "{\"access_token\":\"first-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
        responseStatus = 200;
        tokenServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tokenServer.createContext("/token", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(Arrays.stream(body.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(parts -> decode(parts[0]), parts -> decode(parts[1]))));
            byte[] response = tokenResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        tokenServer.start();
        ClientRegistration registration = ClientRegistration.withRegistrationId("system-user")
            .clientId("pcs")
            .clientSecret("client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(new AuthorizationGrantType("password"))
            .tokenUri("http://127.0.0.1:" + tokenServer.getAddress().getPort() + "/token")
            .scope("openid")
            .build();
        var repository = new InMemoryClientRegistrationRepository(registration);
        manager = new OAuth2ClientConfig().authorizedClientManager(
            repository, new InMemoryOAuth2AuthorizedClientService(repository));
    }

    @AfterEach
    void tearDown() {
        tokenServer.stop(0);
    }

    @Test
    void obtainsAndCachesAnIdamPasswordGrantToken() {
        var first = manager.authorize(authorizeRequest());
        var cached = manager.authorize(authorizeRequest());

        assertThat(first.getAccessToken().getTokenValue()).isEqualTo("first-token");
        assertThat(cached.getAccessToken()).isEqualTo(first.getAccessToken());
        assertThat(requests).containsExactly(Map.of(
            "grant_type", "password", "username", "user@example.test", "password", "p&ss word",
            "client_id", "pcs", "client_secret", "client-secret", "scope", "openid"));
    }

    @Test
    void reacquiresAnExpiringTokenWithoutARefreshToken() {
        tokenResponse = "{\"access_token\":\"expiring\",\"token_type\":\"Bearer\",\"expires_in\":30}";
        manager.authorize(authorizeRequest());
        tokenResponse = "{\"access_token\":\"replacement\",\"token_type\":\"Bearer\",\"expires_in\":3600}";

        assertThat(manager.authorize(authorizeRequest()).getAccessToken().getTokenValue())
            .isEqualTo("replacement");
        assertThat(requests).hasSize(2).allSatisfy(request ->
            assertThat(request).containsEntry("grant_type", "password"));
    }

    @Test
    void delegatesExpiringTokensToTheRefreshGrant() {
        tokenResponse = """
            {"access_token":"expiring","token_type":"Bearer","expires_in":30,"refresh_token":"refresh-me"}
            """;
        manager.authorize(authorizeRequest());
        tokenResponse = "{\"access_token\":\"refreshed\",\"token_type\":\"Bearer\",\"expires_in\":3600}";

        assertThat(manager.authorize(authorizeRequest()).getAccessToken().getTokenValue()).isEqualTo("refreshed");
        assertThat(requests).hasSize(2);
        assertThat(requests.getLast()).containsEntry("grant_type", "refresh_token")
            .containsEntry("refresh_token", "refresh-me").doesNotContainKeys("username", "password");
    }

    @Test
    void rejectsAnIdamErrorResponse() {
        responseStatus = 400;
        tokenResponse = "{\"error\":\"invalid_grant\"}";

        assertThatThrownBy(() -> manager.authorize(authorizeRequest()))
            .isInstanceOf(OAuth2AuthorizationException.class);
    }

    private OAuth2AuthorizeRequest authorizeRequest() {
        return OAuth2AuthorizeRequest.withClientRegistrationId("system-user")
            .principal("user@example.test")
            .attribute(PasswordGrantOAuth2AuthorizedClientProvider.USERNAME_ATTRIBUTE_NAME, "user@example.test")
            .attribute(PasswordGrantOAuth2AuthorizedClientProvider.PASSWORD_ATTRIBUTE_NAME, "p&ss word")
            .build();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
