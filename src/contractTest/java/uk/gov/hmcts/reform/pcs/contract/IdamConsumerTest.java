package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.models.TokenRequest;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration({FeignAutoConfiguration.class, FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class})
@EnableFeignClients(clients = IdamApi.class)
@TestPropertySource(properties = "idam.api.url=http://localhost:5000")
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "idamApi_oidc", port = "5000")

@Disabled("Do not publish this pact until SIDM-9833 is resolved")
public class IdamConsumerTest {

    @Autowired
    private IdamApi idamApi;

    static {
        System.setProperty("pact.specification.version", "3");
    }

    @Pact(provider = "idamApi_oidc", consumer = "pcs_api")
    public V4Pact requestToken(PactDslWithProvider builder) throws JsonProcessingException {

        return builder
            .given("a token is requested")
            .uponReceiving("a request to get the access token")
            .path("/o/token")
            .method(HttpMethod.POST.toString())
            .body("redirect_uri=http%3A%2F%2Fwww.dummy-pact-service.com%2Fcallback"
                      + "&client_id=pcs_api"
                      + "&grant_type=authorization_code"
                      + "&username=caseworker@fake.hmcts.net"
                      + "&password=password"
                      + "&client_secret=AAAAAA"
                      + "&scope=openid profile roles",
                  "application/x-www-form-urlencoded")
            .willRespondWith()
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .status(HttpStatus.OK.value())
            .body(new PactDslJsonBody()
                      .stringType("access_token"))
            .toPact(V4Pact.class);
    }

    private TokenRequest buildTokenRequest() {
        return new TokenRequest(
            "pcs_api",
            "AAAAAA",
            "authorization_code",
            "http://www.dummy-pact-service.com/callback",
            "caseworker@fake.hmcts.net",
            "password",
            "openid profile roles",
            null, null);
    }

    @Pact(provider = "idamApi_oidc", consumer = "pcs_api")
    public V4Pact requestUserInfo(PactDslWithProvider builder) throws JsonProcessingException {

        return builder
            .given("userinfo is requested")
            .uponReceiving("a request to get the the user information")
            .path("/o/userinfo")
            .headers(HttpHeaders.AUTHORIZATION, "Bearer authorisationToken")
            .method(HttpMethod.GET.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createUserDetailsResponse())
            .toPact(V4Pact.class);
    }

    static PactDslJsonBody createUserDetailsResponse() {
        return new PactDslJsonBody()
            .stringType("uid", "1111-2222-3333-4567")
            .stringType("sub", "caseofficer@fake.hmcts.net")
            .stringValue("givenName", "Case")
            .stringValue("familyName", "Officer")
            .minArrayLike("roles", 1, PactDslJsonRootValue.stringType("caseworker"),1)
            .stringType("IDAM_ADMIN_USER", "idamAdminUser");
    }

    @Test
    @PactTestFor(pactMethods = {"requestToken", "requestUserInfo"})
    void verifyAllPacts() {
        TokenResponse tokenResponse = idamApi.generateOpenIdToken(buildTokenRequest());
        assertThat(tokenResponse.accessToken).isNotBlank();

        var userDetails = idamApi.retrieveUserInfo("Bearer authorisationToken");
        assertThat(userDetails.getSub()).isEqualTo("caseofficer@fake.hmcts.net");
        assertThat(userDetails.getUid()).isEqualTo("1111-2222-3333-4567");
        assertThat(userDetails.getRoles()).contains("caseworker");
    }
}
