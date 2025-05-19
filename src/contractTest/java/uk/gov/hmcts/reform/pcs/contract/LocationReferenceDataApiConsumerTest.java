package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.api.LocationReferenceApi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoConfiguration
@TestPropertySource(properties = "refdata.location.url=http://localhost:8089")
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
@PactTestFor(providerName = "referenceData_location", port = "8089")
@SpringBootTest(classes = {LocationReferenceApi.class, DisableFlywayConfig.class})
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.idam.client", "uk.gov.hmcts.reform.pcs.location.service.api"})

public class LocationReferenceDataApiConsumerTest {

    private static final String SERVICE_AUTH_TOKEN = "Bearer serviceToken";
    private static final String AUTHORIZATION_TOKEN = "Bearer userToken";

    @Autowired
    private LocationReferenceApi locationReferenceApi;

    @Pact(provider = "referenceData_location", consumer = "pcs_api")
    public V4Pact getCourtVenueByEpimmsIdAndType(PactDslWithProvider builder) throws IOException {
        return builder
            .given("Court Venues exist for the input request provided")
            .uponReceiving("valid request to retrieve court venue")
            .path("/refdata/location/court-venues")
            .method(HttpMethod.GET.toString())
            .headers("ServiceAuthorization", SERVICE_AUTH_TOKEN,
                     "Authorization", AUTHORIZATION_TOKEN)
            .matchQuery("court_type_id", String.valueOf(PactDslJsonRootValue.integerType(17)))
            .matchQuery("epimms_id", "123456789")
            .willRespondWith()
            .status(200)
            .headers(Map.of(HttpHeaders.CONTENT_TYPE, "application/json"))
            .body(buildLocationRefDataResponseBody())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getCourtVenueByEpimmsIdAndType")
    void verifyCourtVenueByEpimmsIdAndType() {
        List<CourtVenue> response = locationReferenceApi.getCountyCourts(
            AUTHORIZATION_TOKEN, SERVICE_AUTH_TOKEN, "123456789", 17
        );

        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();

        CourtVenue location = response.get(0);

        assertThat(location.epimmsId()).isEqualTo(123456789);
        assertThat(location.courtVenueId()).isEqualTo(1L);
        assertThat(location.courtName()).isEqualTo("ABERDEEN TRIBUNAL HEARING CENTRE 1");
    }

    static DslPart buildLocationRefDataResponseBody() {
        return new PactDslJsonArray()
            .object()
            .stringType("epimms_id", "123456789")
            .stringType("court_venue_id", "1")
            .stringType("court_name", "ABERDEEN TRIBUNAL HEARING CENTRE 1")
            .closeObject()
            .object()
            .stringType("epimms_id", "123456789")
            .stringType("court_venue_id", "2")
            .stringType("court_name", "GLASGOW TRIBUNAL HEARING CENTRE")
            .closeObject();
    }
}
