package uk.gov.hmcts.reform.pcs.security;

import com.github.benmanes.caffeine.cache.Ticker;
import com.sun.net.httpserver.HttpServer;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.OAuth2Configuration;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.exception.IdamException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class IdamTokenProviderTest {

    private HttpServer server;
    private ApplicationContextRunner contextRunner;
    private final List<Map<String, String>> requests = new CopyOnWriteArrayList<>();
    private volatile String responseBody;
    private volatile int responseStatus;

    @BeforeEach
    void setUp() throws IOException {
        responseBody = tokenResponse("first-token");
        responseStatus = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/o/token", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(Arrays.stream(body.split("&"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(parts -> decode(parts[0]), parts -> decode(parts[1]))));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ClientConfiguration.class)
            .withPropertyValues(
                "idam.api.url=http://127.0.0.1:" + server.getAddress().getPort(),
                "idam.client.id=pcs-api",
                "idam.client.secret=client-secret",
                "idam.client.scope=openid profile roles",
                "idam.system-user.username=system@example.test",
                "idam.system-user.password=p&ss word",
                "idam.prd-admin.username=admin@example.test",
                "idam.prd-admin.password=admin-secret",
                "spring.http.converters.preferred-json-mapper=jackson2");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void obtainsAndCachesTheSystemUserTokenThroughTheHmctsClient() {
        contextRunner.run(context -> {
            var provider = context.getBean("systemUpdateUserTokenProvider", IdamTokenProvider.class);

            assertThat(provider.getAuthToken()).isEqualTo("Bearer first-token");
            assertThat(provider.getAuthToken()).isEqualTo("Bearer first-token");
            assertThat(requests).singleElement().satisfies(request -> assertThat(request).containsAllEntriesOf(Map.of(
                "client_id", "pcs-api", "client_secret", "client-secret", "grant_type", "password",
                "username", "system@example.test", "password", "p&ss word", "scope", "openid profile roles")));
        });
    }

    @Test
    void keepsTheTwoServiceIdentitiesAndTheirCachedTokensSeparate() {
        contextRunner.run(context -> {
            var systemUser = context.getBean("systemUpdateUserTokenProvider", IdamTokenProvider.class);
            var adminUser = context.getBean("prdAdminTokenProvider", IdamTokenProvider.class);
            assertThat(systemUser.getAuthToken()).isEqualTo("Bearer first-token");
            responseBody = tokenResponse("admin-token");

            assertThat(adminUser.getAuthToken()).isEqualTo("Bearer admin-token");
            assertThat(systemUser.getAuthToken()).isEqualTo("Bearer first-token");
            assertThat(adminUser.getAuthToken()).isEqualTo("Bearer admin-token");
            assertThat(requests).hasSize(2);
            assertThat(requests.getLast()).containsEntry("username", "admin@example.test")
                .containsEntry("password", "admin-secret");
        });
    }

    @Test
    void reacquiresTheTokenSixtySecondsBeforeItsExpiry() {
        contextRunner.run(context -> {
            var ticker = new FakeTicker();
            var provider = new IdamTokenProvider(
                context.getBean(IdamClient.class), "system@example.test", "secret", ticker);
            assertThat(provider.getAuthToken()).isEqualTo("Bearer first-token");
            ticker.advance(Duration.ofSeconds(3539));
            assertThat(provider.getAuthToken()).isEqualTo("Bearer first-token");
            responseBody = tokenResponse("renewed-token");
            ticker.advance(Duration.ofSeconds(1));

            assertThat(provider.getAuthToken()).isEqualTo("Bearer renewed-token");
            assertThat(requests).hasSize(2).allSatisfy(request -> assertThat(request)
                .containsEntry("grant_type", "password").doesNotContainKey("refresh_token"));
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {120, 7200})
    void respectsTheLifetimeReturnedByIdam(int lifetimeSeconds) {
        contextRunner.run(context -> {
            var ticker = new FakeTicker();
            var provider = new IdamTokenProvider(
                context.getBean(IdamClient.class), "system@example.test", "secret", ticker);
            responseBody = "{\"access_token\":\"first-token\",\"expires_in\":" + lifetimeSeconds + "}";
            assertThat(provider.getAuthToken()).isEqualTo("Bearer first-token");
            ticker.advance(Duration.ofSeconds(lifetimeSeconds - 61));
            assertThat(provider.getAuthToken()).isEqualTo("Bearer first-token");
            responseBody = tokenResponse("renewed-token");
            ticker.advance(Duration.ofSeconds(1));

            assertThat(provider.getAuthToken()).isEqualTo("Bearer renewed-token");
            assertThat(requests).hasSize(2);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 60})
    void doesNotCacheTokensAlreadyWithinTheExpiryMargin(int lifetimeSeconds) {
        contextRunner.run(context -> {
            var provider = context.getBean("systemUpdateUserTokenProvider", IdamTokenProvider.class);
            responseBody = "{\"access_token\":\"short-token\",\"expires_in\":" + lifetimeSeconds + "}";
            assertThat(provider.getAuthToken()).isEqualTo("Bearer short-token");
            responseBody = tokenResponse("renewed-token");

            assertThat(provider.getAuthToken()).isEqualTo("Bearer renewed-token");
            assertThat(requests).hasSize(2);
        });
    }

    @Test
    void concurrentCallersShareOneTokenRequest() {
        contextRunner.run(context -> {
            var provider = context.getBean("systemUpdateUserTokenProvider", IdamTokenProvider.class);
            var start = new CountDownLatch(1);
            var executor = Executors.newFixedThreadPool(8);
            try {
                var results = IntStream.range(0, 8).mapToObj(index -> executor.submit(() -> {
                    start.await();
                    return provider.getAuthToken();
                })).toList();
                start.countDown();

                for (var result : results) {
                    assertThat(result.get(10, TimeUnit.SECONDS)).isEqualTo("Bearer first-token");
                }
                assertThat(requests).hasSize(1);
            } finally {
                executor.shutdownNow();
            }
        });
    }

    @Test
    void failedRenewalDoesNotReturnAnExpiredTokenAndCanBeRetried() {
        contextRunner.run(context -> {
            var ticker = new FakeTicker();
            var provider = new IdamTokenProvider(
                context.getBean(IdamClient.class), "system@example.test", "secret", ticker);
            assertThat(provider.getAuthToken()).isEqualTo("Bearer first-token");
            ticker.advance(Duration.ofHours(1));
            responseStatus = 503;
            responseBody = "{\"error\":\"temporarily_unavailable\"}";

            assertThat(catchThrowableOfType(IdamException.class, provider::getAuthToken))
                .hasCauseInstanceOf(FeignException.class);
            responseStatus = 200;
            responseBody = tokenResponse("recovered-token");
            assertThat(provider.getAuthToken()).isEqualTo("Bearer recovered-token");
            assertThat(requests).hasSize(3);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 429, 500, 503})
    void preservesTheUpstreamHttpFailureForTheExistingErrorHandler(int status) {
        contextRunner.run(context -> {
            responseStatus = status;
            responseBody = "{\"error\":\"invalid_grant\"}";
            var provider = context.getBean("systemUpdateUserTokenProvider", IdamTokenProvider.class);

            IdamException failure = catchThrowableOfType(IdamException.class, provider::getAuthToken);

            assertThat(failure).hasMessage("Unable to get access token response")
                .hasCauseInstanceOf(FeignException.class);
            assertThat(((FeignException) failure.getCause()).status()).isEqualTo(status);
            responseStatus = 200;
            responseBody = tokenResponse("recovered-token");
            assertThat(provider.getAuthToken()).isEqualTo("Bearer recovered-token");
            assertThat(requests).hasSize(2);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"access_token\":\"\",\"expires_in\":3600}",
        "{\"access_token\":\"token\"}", "{\"access_token\":\"token\",\"expires_in\":0}",
        "{\"access_token\":\"token\",\"expires_in\":-1}", "{\"access_token\":\"token\",\"expires_in\":\"invalid\"}"
    })
    void rejectsInvalidTokenResponsesWithoutCachingThem(String body) {
        contextRunner.run(context -> {
            responseBody = body;
            var provider = context.getBean("systemUpdateUserTokenProvider", IdamTokenProvider.class);

            assertThat(catchThrowableOfType(IdamException.class, provider::getAuthToken))
                .hasMessage("Unable to get access token response");
            responseBody = tokenResponse("valid-token");
            assertThat(provider.getAuthToken()).isEqualTo("Bearer valid-token");
            assertThat(requests).hasSize(2);
        });
    }

    private static String tokenResponse(String token) {
        return "{\"access_token\":\"" + token + "\",\"expires_in\":3600,\"refresh_token\":\"unused\"}";
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableFeignClients(clients = IdamApi.class)
    @Import({IdamTokenProviderConfig.class, IdamClient.class, OAuth2Configuration.class, JacksonConfiguration.class})
    @SuppressWarnings("removal")
    @ImportAutoConfiguration({FeignAutoConfiguration.class, FeignClientsConfiguration.class,
        Jackson2AutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
    static class ClientConfiguration {
    }

    private static class FakeTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }

        @Override
        public long read() {
            return nanos.get();
        }
    }
}
