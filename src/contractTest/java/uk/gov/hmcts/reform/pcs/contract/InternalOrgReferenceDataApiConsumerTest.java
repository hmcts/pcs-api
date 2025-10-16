package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.contract.LocationReferenceDataApiConsumerTest.AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.pcs.contract.LocationReferenceDataApiConsumerTest.SERVICE_AUTH_TOKEN;


public class InternalOrgReferenceDataApiConsumerTest {

    public static final String ORG_ID = "OrgId";

    @Autowired
    private OrganisationApi organisationApi;

    @Pact(provider = "referenceData_organisationalInternal", consumer = "pcs_api")
    public V4Pact getOrganisationById(PactDslWithProvider builder) throws IOException {
        return builder
            .given("organisation exists for given Id")
            .uponReceiving("a request to get an organisation by id")
            .path("/refdata/internal/v1/organisations")
            .method(HttpMethod.GET.toString())
            .headers("ServiceAuthorization", SERVICE_AUTH_TOKEN,
                     "Authorization", AUTHORIZATION_TOKEN)
            //Need assistance here
            .matchQuery("id", String.valueOf(PactDslJsonRootValue.integerType(17)))

            .willRespondWith()
            .status(200)
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(buildOrganisationResponseDsl())
            .toPact(V4Pact.class);
    }

    //Need assistance
    @Test
    @PactTestFor(pactMethod = "getOrganisationById")
    void verifyInternalOrganisation() {
        OrganisationResponse response =
            organisationApi.findOrganisationById(AUTHORIZATION_TOKEN, SERVICE_AUTH_TOKEN, ORG_ID);

        assertThat(response.getOrganisationIdentifier().isEqualTo(""));
        assertThat(response.name().isEqualTo("Possessions Claims Solicitor Org");
        }

    }








}
