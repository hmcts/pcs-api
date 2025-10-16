package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import java.io.IOException;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@PactTestFor(providerName = "referenceData_location", port = "6668")

public class InternalOrgReferenceDataApiConsumerTest {

    private static final String SERVICE_AUTH_TOKEN = "Bearer serviceToken";
    private static final String AUTHORIZATION_TOKEN = "Bearer userToken";

    @Autowired
    private RdProfessionalApi rdProfessionalApi;

    @Pact(provider = "referenceData_organisationalDetailsInternal", consumer = "pcs_api")
    public V4Pact getOrganisationById(PactDslWithProvider builder) throws IOException {
        return builder
            .given("organisation exists for given Id")
            .uponReceiving("a request to get an organisation by id")
            .path("/refdata/internal/v1/organisations/orgDetails/4d318f91-5591-44e3-870b-ad5616f65627")
            .method(HttpMethod.GET.toString())
            .headers("ServiceAuthorization", SERVICE_AUTH_TOKEN,
                     "Authorization", AUTHORIZATION_TOKEN)
            .willRespondWith()
            .status(200)
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(buildOrganisationResponseDsl());
            .toPact(V4Pact.class);
    }

    //Need assistance
    @Test
    @PactTestFor(pactMethod = "getOrganisationById")
    void verifyInternalOrganisation() {
        OrganisationResponse response =
            rdProfessionalApi.getOrganisationDetails(AUTHORIZATION_TOKEN, SERVICE_AUTH_TOKEN, ORG_ID);

        assertThat(response.getOrganisationIdentifier().isEqualTo(""));
        assertThat(response.name().isEqualTo("Possessions Claims Solicitor Org");
    }

}

static DslPart buildOrganisationResponseDsl() {
    return new PactDslJsonArray()
        .object()
        .stringType("companyNumber", "companyNumber")
        .stringType("companyUrl", "companyUrl")
        .minArrayLike("contactInformation", 1, 1,
                      sh -> {
                          sh.stringType("addressLine1", "addressLine1")
                              .stringType("addressLine2", "addressLine2")
                              .stringType("country", "UK")
                              .stringType("postCode", "SM12SX");
                      }
        )
        .closeObject()
        .stringType("name", "companyName")
        .stringType("organisationIdentifier", "2")
        .stringType("court_name", "GLASGOW TRIBUNAL HEARING CENTRE")
        .closeObject();







}
