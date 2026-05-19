package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import uk.gov.hmcts.reform.pcs.idam.IdamUserInfoApi;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration({FeignAutoConfiguration.class, FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class})
@EnableFeignClients(clients = IdamUserInfoApi.class)
@TestPropertySource(properties = "idam.api.url=http://localhost:5000")
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "idamApi_oidc", port = "5000")
public class IdamConsumerTest {

    @Autowired
    private IdamUserInfoApi idamUserInfoApi;

    @Pact(provider = "idamApi_oidc", consumer = "pcs_api")
    public V4Pact requestUserInfo(PactDslWithProvider builder) throws JsonProcessingException {

        return builder
            .given("userinfo is requested")
            .uponReceiving("a request to get the user information")
            .path("/o/userinfo")
            .headers(HttpHeaders.AUTHORIZATION, "Bearer authorisationToken")
            .method(HttpMethod.GET.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(createUserInfoResponse())
            .toPact(V4Pact.class);
    }

    static PactDslJsonBody createUserInfoResponse() {
        return new PactDslJsonBody()
            .stringType("uid", "1111-2222-3333-4567")
            .stringType("sub", "caseofficer@fake.hmcts.net")
            .stringType("given_name", "Case")
            .stringType("family_name", "Officer")
            .minArrayLike("roles", 1, PactDslJsonRootValue.stringType("caseworker"), 1);
    }

    @Test
    @PactTestFor(pactMethods = "requestUserInfo")
    void verifyUserInfoPact() {
        UserInfo userInfo = idamUserInfoApi.getUserInfo("Bearer authorisationToken");

        assertThat(userInfo.getSub()).isEqualTo("caseofficer@fake.hmcts.net");
        assertThat(userInfo.getUid()).isEqualTo("1111-2222-3333-4567");
        assertThat(userInfo.getGivenName()).isEqualTo("Case");
        assertThat(userInfo.getFamilyName()).isEqualTo("Officer");
        assertThat(userInfo.getRoles()).contains("caseworker");
    }
}
