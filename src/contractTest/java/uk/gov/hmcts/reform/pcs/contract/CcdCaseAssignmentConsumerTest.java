package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRoleWithOrganisation;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseAssignmentUserRolesResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class
})
@EnableFeignClients(clients = CaseAssignmentApi.class)
@TestPropertySource(properties = "core_case_data.api.url=http://localhost:4452")
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "ccdDataStoreAPI_caseAssignedUserRoles", port = "4452")

public class CcdCaseAssignmentConsumerTest {

    private static final String SERVICE_AUTH_TOKEN = "Bearer serviceToken";
    private static final String AUTHORIZATION_TOKEN = "Bearer userToken";
    private static final String CASE_ID = "1764062392941112";
    private static final String USER_ID = "9a2d861a-6264-4765-9f61-1d403079f71b";
    private static final String CASE_ROLE = "[DEFENDANT]";

    @Autowired
    private CaseAssignmentApi caseAssignmentService;

    @Pact(provider = "ccdDataStoreAPI_caseAssignedUserRoles", consumer = "pcs_api")
    public V4Pact assignCaseRole(PactDslWithProvider builder) throws JsonProcessingException {

        return builder
            .given("A User Role exists for a Case")
            .uponReceiving("a request to add a user role")
            .path("/case-users")
            .method("POST")
            .headers("ServiceAuthorization", SERVICE_AUTH_TOKEN,
                     "Authorization", AUTHORIZATION_TOKEN,
                     "Content-Type", "application/json")
            .body(caseRoleBody())
            .willRespondWith()
            .status(201)
            .toPact(V4Pact.class);
    }

    @Pact(provider = "ccdDataStoreAPI_caseAssignedUserRoles", consumer = "pcs_api")
    public V4Pact fetchCaseRole(PactDslWithProvider builder) throws JsonProcessingException {

        return builder
            .given("A User Role exists for a Case")
            .uponReceiving("a request to get a user role")
            .path("/case-users")
            .matchQuery("case_ids", CASE_ID)
            .method("GET")
            .headers("ServiceAuthorization", SERVICE_AUTH_TOKEN,
                     "Authorization", AUTHORIZATION_TOKEN)
            .willRespondWith()
            .status(200)
            .body(caseRoleBody())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethods = {"assignCaseRole", "fetchCaseRole"})
    void verifyAllPacts() {
        CaseAssignmentUserRolesRequest request =
            CaseAssignmentUserRolesRequest.builder()
                .caseAssignmentUserRolesWithOrganisation(
                    List.of(CaseAssignmentUserRoleWithOrganisation.builder()
                                .caseDataId(CASE_ID)
                                .userId(USER_ID)
                                .caseRole(CASE_ROLE)
                                .build()
                    )
                )
                .build();

        caseAssignmentService.addCaseUserRoles(
            AUTHORIZATION_TOKEN,
            SERVICE_AUTH_TOKEN,
            request
        );

        CaseAssignmentUserRolesResource response = caseAssignmentService.getUserRoles(
            AUTHORIZATION_TOKEN,
            SERVICE_AUTH_TOKEN,
            List.of(CASE_ID)
        );
        assertThat(response.getCaseAssignmentUserRoles().get(0).getUserId()).isEqualTo(USER_ID);
        assertThat(response.getCaseAssignmentUserRoles().get(0).getCaseRole()).isEqualTo(CASE_ROLE);
        assertThat(response.getCaseAssignmentUserRoles().get(0).getCaseDataId()).isEqualTo(CASE_ID);
    }

    static PactDslJsonBody caseRoleBody() {
        return (PactDslJsonBody) new PactDslJsonBody()
            .minArrayLike("case_users", 1)  // must match CCD DTO field
            .stringType("case_id", CASE_ID)
            .stringType("case_role", CASE_ROLE)
            .stringType("user_id", USER_ID)
            .closeObject()
            .closeArray();
    }
}
