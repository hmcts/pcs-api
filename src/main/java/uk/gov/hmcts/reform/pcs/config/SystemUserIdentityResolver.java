package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the system user's IDAM identity (uid and names) from their credentials, the same
 * token + userinfo lookup the rest of the codebase uses, so the uid never has to be provisioned
 * per environment and cannot go stale if the IDAM account is recreated.
 */
public class SystemUserIdentityResolver {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record SystemUserIdentity(String id, String firstName, String lastName) {
    }

    public SystemUserIdentityResolver(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public SystemUserIdentityResolver() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    public SystemUserIdentity resolve(String idamApiUrl, String username, String password,
                                      String clientId, String clientSecret) throws Exception {
        String accessToken = accessToken(idamApiUrl, username, password, clientId, clientSecret);
        JsonNode userInfo = objectMapper.readTree(get(idamApiUrl + "/o/userinfo", accessToken));
        return new SystemUserIdentity(
            userInfo.path("uid").asText(),
            userInfo.path("given_name").asText(null),
            userInfo.path("family_name").asText(null)
        );
    }

    private String accessToken(String idamApiUrl, String username, String password,
                               String clientId, String clientSecret) throws Exception {
        String form = Map.of(
                "grant_type", "password",
                "username", username,
                "password", password,
                "client_id", clientId,
                "client_secret", clientSecret,
                "scope", "openid profile roles"
            ).entrySet().stream()
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder(URI.create(idamApiUrl + "/o/token"))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("IDAM token request failed with status " + response.statusCode());
        }
        return objectMapper.readTree(response.body()).path("access_token").asText();
    }

    private String get(String url, String bearerToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("Authorization", "Bearer " + bearerToken)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("IDAM userinfo request failed with status " + response.statusCode());
        }
        return response.body();
    }
}
