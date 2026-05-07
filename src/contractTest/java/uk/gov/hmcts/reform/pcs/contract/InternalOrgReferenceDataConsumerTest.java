package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.io.IOException;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})
@EnableFeignClients(clients = RdProfessionalApi.class)
@Import(RdProfessionalApi.class)
@TestPropertySource(properties = "rd-professional.api-url=http://localhost:6668")
@ExtendWith({PactConsumerTestExt.class, SpringExtension.class})
@PactTestFor(providerName = "referenceData_organisationalDetailsInternal", port = "6668")
@RequiredArgsConstructor
@TestConstructor(autowireMode = ALL)

public class InternalOrgReferenceDataConsumerTest {

    private static final String SERVICE_AUTH_TOKEN = "Bearer serviceToken";
    private static final String AUTHORIZATION_TOKEN = "Bearer userToken";
    private static final String USER_ID = "4d318f91-5591-44e3-870b-ad5616f65627";

    private final RdProfessionalApi rdProfessionalApi;

    @Pact(provider = "referenceData_organisationalDetailsInternal", consumer = "pcs_api")
    public V4Pact getOrganisationById(PactDslWithProvider builder) throws IOException {
        return builder
            .given("Organisation exists for given Id")
            .uponReceiving("a request to get an organisation by id")
            .path("/refdata/internal/v1/organisations/orgDetails/" + USER_ID)
            .method(HttpMethod.GET.toString())
            .headers("ServiceAuthorization", SERVICE_AUTH_TOKEN,
                     "Authorization", AUTHORIZATION_TOKEN)
                .willRespondWith()
                .status(200)
                .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(buildOrganisationResponseDsl())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getOrganisationById")
    public void verifyInternalOrganisation() {
        OrganisationDetailsResponse response =
            rdProfessionalApi.getOrganisationDetails(USER_ID, SERVICE_AUTH_TOKEN, AUTHORIZATION_TOKEN);

        assertThat(response.getName()).isEqualTo("Possession Claims Solicitor Org");
    }

    static PactDslJsonBody buildOrganisationResponseDsl() {
        return (PactDslJsonBody) new PactDslJsonBody()
            .stringType("name", "Possession Claims Solicitor Org")
            .stringType("organisationIdentifier", "E71FH4Q")
            .stringType("dateReceived", "2025-09-11T13:46:54.42977")
            .stringType("dateApproved", "2025-09-11T13:56:40.778072")
            .stringType("lastUpdated", "2025-09-11T13:56:40.77853")
            .stringType("status", "ACTIVE")
            .booleanType("sraRegulated", false)
            .object("superUser")
            .stringType("firstName", "Solicitor")
            .stringType("lastName", "Admin Org")
            .stringType("email", "pcs-solicitor-org-adm@mailinator.com")
            .closeObject()
            .array("contactInformation")
            .object()
            .stringType("addressId", "98b33d54-2a0b-4da0-8b8c-5215b0fc114b")
            .stringType("created", "2025-09-11T13:46:54.529947")
            .stringType("addressLine1", "Ministry Of Justice")
            .stringType("addressLine2", "Seventh Floor 102 Petty France")
            .stringType("townCity", "London")
            .stringType("country", "United Kingdom")
            .stringType("postCode", "SW1H 9AJ")
            .closeObject()
            .closeArray()
            .array("paymentAccount")
            .stringValue("PBA0078010")
            .stringValue("PBA0078011")
            .closeArray();
    }
}
