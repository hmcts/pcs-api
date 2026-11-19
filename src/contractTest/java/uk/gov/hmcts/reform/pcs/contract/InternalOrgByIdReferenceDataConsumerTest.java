package uk.gov.hmcts.reform.pcs.contract;

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
@TestPropertySource(properties = "rd-professional.api-url=http://localhost:6669")
@ExtendWith({PactConsumerTestExt.class, SpringExtension.class})
@PactTestFor(providerName = "referenceData_organisationalDetailsInternal", port = "6669")
@RequiredArgsConstructor
@TestConstructor(autowireMode = ALL)
public class InternalOrgByIdReferenceDataConsumerTest {

    private static final String SERVICE_AUTH_TOKEN = "Bearer serviceToken";
    private static final String AUTHORIZATION_TOKEN = "Bearer userToken";
    private static final String ORGANISATION_ID = "E71FH4Q";

    private final RdProfessionalApi rdProfessionalApi;

    @Pact(provider = "referenceData_organisationalDetailsInternal", consumer = "pcs_api")
    public V4Pact getOrganisationByOrganisationIdentifier(PactDslWithProvider builder) {
        return builder
            .given("Organisation exists for given Id")
            .uponReceiving("a request to get an organisation by organisation identifier")
            .path("/refdata/internal/v1/organisations")
            .query("id=" + ORGANISATION_ID)
            .method(HttpMethod.GET.toString())
            .headers(
                "ServiceAuthorization", SERVICE_AUTH_TOKEN,
                "Authorization", AUTHORIZATION_TOKEN
            )
            .willRespondWith()
            .status(200)
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(InternalOrgReferenceDataConsumerTest.buildOrganisationResponseDsl())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getOrganisationByOrganisationIdentifier")
    public void verifyInternalOrganisationByOrganisationIdentifier() {
        OrganisationDetailsResponse response =
            rdProfessionalApi.getOrganisationById(SERVICE_AUTH_TOKEN, AUTHORIZATION_TOKEN, ORGANISATION_ID);

        assertThat(response.getOrganisationIdentifier()).isEqualTo("E71FH4Q");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getPaymentAccount()).containsExactly("PBA0078010", "PBA0078011");
    }
}
