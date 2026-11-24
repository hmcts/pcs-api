package uk.gov.hmcts.reform.pcs.bulkclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.bulkclaim.BulkClaimSoapTestSupport.NAMESPACE;
import static uk.gov.hmcts.reform.pcs.bulkclaim.BulkClaimSoapTestSupport.SERVICE_PATH;
import static uk.gov.hmcts.reform.pcs.bulkclaim.BulkClaimSoapTestSupport.envelope;
import static uk.gov.hmcts.reform.pcs.bulkclaim.BulkClaimSoapTestSupport.get;
import static uk.gov.hmcts.reform.pcs.bulkclaim.BulkClaimSoapTestSupport.post;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"security.anonymousPaths[0]=/health", "bulk-claims.max-request-size=4KB"})
@ActiveProfiles("integration")
@DisplayName("Bulk claims SOAP service integration tests")
class BulkClaimSoapIT extends AbstractPostgresContainerIT {

    private static final String APIM_TOKEN = "Bearer apim-token";
    private static final String XML = "text/xml;charset=UTF-8";
    private static final String[] APIM = {"ServiceAuthorization", APIM_TOKEN, "X-Organisation-Id", "ORG123"};

    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @MockitoBean
    private FeatureToggleService featureToggleService;

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @BeforeEach
    void setUp() {
        when(serviceAuthorisationApi.getServiceName(APIM_TOKEN)).thenReturn("pcs_bulk_claims_apim");
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2_MORTGAGE)).thenReturn(true);
    }

    @Test
    void shouldReportSchemaInvalidInTheReturnStringForAnAxisStyleRequest() throws Exception {
        HttpResponse<String> response = post(port, SERVICE_PATH, XML,
            envelope("processRentalClaim", "rentalRequest", "<RentalClaimSet/>"),
            "SOAPAction", "urn:processRentalClaim", "ServiceAuthorization", APIM_TOKEN,
            "X-Organisation-Id", "ORG123");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
            .contains("<ns2:processRentalClaimResponse xmlns:ns2=\"" + NAMESPACE + "\">")
            .contains("processRentalClaimReturn")
            .contains("SCHEMA_INVALID")
            .doesNotContain("Fault");
    }

    @Test
    void shouldAnswerAStatusRequestForAnUnknownClaimInTheReturnString() throws Exception {
        HttpResponse<String> response = post(port, SERVICE_PATH, XML,
            envelope("getClaimStatus", "customerUniqueIdentifier", "NOPE"), APIM);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("getClaimStatusReturn").contains("&lt;FailReasonCode&gt;17");
    }

    @Test
    void shouldAnswerAStatusListRequest() throws Exception {
        String request = "<BulkClaimStatusRequest xmlns=\"http://hmcts.gov.uk/pcs/bulkclaim/v2\">"
            + "<CustomerUniqueIdentifier>A</CustomerUniqueIdentifier></BulkClaimStatusRequest>";

        HttpResponse<String> response = post(port, SERVICE_PATH, XML,
            envelope("getClaimStatuses", "statusRequest", request), APIM);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("getClaimStatusesReturn").contains("BulkClaimStatusListResponse");
    }

    @Test
    void shouldFaultForAMortgageRequestWhenTheMortgageFlagIsOff() throws Exception {
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2_MORTGAGE)).thenReturn(false);

        HttpResponse<String> response = post(port, SERVICE_PATH, XML,
            envelope("processMortgageClaim", "mortgageRequest", "<a/>"), APIM);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("Client").contains("Operation processMortgageClaim is not available");
    }

    @Test
    void shouldFaultForAnUnknownOperation() throws Exception {
        HttpResponse<String> response = post(port, SERVICE_PATH, XML,
            envelope("cancelClaim", "id", "1"), APIM);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("Client").contains("Unknown operation");
    }

    @Test
    void shouldFaultForAMalformedEnvelope() throws Exception {
        HttpResponse<String> response = post(port, SERVICE_PATH, XML, "<soapenv:Envelope", APIM);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("Client").contains("not a well-formed SOAP message");
    }

    @Test
    void shouldFaultForAnEnvelopeWithADoctype() throws Exception {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE e [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
            + envelope("processRentalClaim", "rentalRequest", "&x;");

        HttpResponse<String> response = post(port, SERVICE_PATH, XML, xxe, APIM);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("Client").doesNotContain("root:");
    }

    @Test
    void shouldFaultForASoap12Request() throws Exception {
        String soap12 = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"><s:Body>"
            + "<ns1:getClaimStatus xmlns:ns1=\"" + NAMESPACE + "\"><ns1:customerUniqueIdentifier>A"
            + "</ns1:customerUniqueIdentifier></ns1:getClaimStatus></s:Body></s:Envelope>";

        HttpResponse<String> response = post(port, SERVICE_PATH, "application/soap+xml;charset=UTF-8", soap12, APIM);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("VersionMismatch").doesNotContain("getClaimStatusResponse");
    }

    @Test
    void shouldRefuseARequestOverTheSizeLimitBeforeParsingIt() throws Exception {
        HttpResponse<String> response = post(port, SERVICE_PATH, XML, "<a>" + "x".repeat(5000) + "</a>", APIM);

        assertThat(response.statusCode()).isEqualTo(413);
    }

    @Test
    void shouldServeTheWsdlWithTheRequestsOwnEndpointAddress() throws Exception {
        for (String path : new String[] {SERVICE_PATH + "?wsdl", "/v2/bulk-claims/services/BulkClaim.wsdl"}) {
            HttpResponse<String> response = get(port, path, APIM);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                .contains("name=\"BulkClaimSoapBinding\"", "style=\"document\"", "name=\"getClaimStatuses\"")
                .contains("location=\"http://localhost:" + port + SERVICE_PATH + "\"");
        }
    }

    @Test
    void shouldServeOnlyTheListedSchemaFiles() throws Exception {
        assertThat(get(port, "/v2/bulk-claims/services/schema/RentalClaim.xsd", APIM).statusCode()).isEqualTo(200);
        assertThat(get(port, "/v2/bulk-claims/services/schema/application.yaml", APIM).statusCode()).isEqualTo(404);
    }

    @Test
    void shouldHideTheServiceAndItsWsdlWhenTheFlagIsOff() throws Exception {
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(false);

        assertThat(post(port, SERVICE_PATH, XML, envelope("getClaimStatus", "customerUniqueIdentifier", "A"), APIM)
            .statusCode()).isEqualTo(404);
        assertThat(get(port, SERVICE_PATH + "?wsdl", APIM).statusCode()).isEqualTo(404);
        assertThat(get(port, "/v2/bulk-claims/services/BulkClaim.wsdl", APIM).statusCode()).isEqualTo(404);
    }
}
