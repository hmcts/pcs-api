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
import static uk.gov.hmcts.reform.pcs.bulkclaim.BulkClaimSoapTestSupport.SERVICE_PATH;
import static uk.gov.hmcts.reform.pcs.bulkclaim.BulkClaimSoapTestSupport.envelope;
import static uk.gov.hmcts.reform.pcs.bulkclaim.BulkClaimSoapTestSupport.post;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "security.anonymousPaths[0]=/health")
@ActiveProfiles("integration")
@DisplayName("Bulk claims APIM S2S lock integration tests")
class BulkClaimApimSecurityIT extends AbstractPostgresContainerIT {

    private static final String APIM_TOKEN = "Bearer apim-token";
    private static final String XUI_TOKEN = "Bearer xui-token";
    private static final String XML = "text/xml;charset=UTF-8";

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
        when(serviceAuthorisationApi.getServiceName(XUI_TOKEN)).thenReturn("xui_webapp");
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);
    }

    @Test
    void shouldReachSoapServiceForApimWithOrganisation() throws Exception {
        HttpResponse<String> response = post(port, SERVICE_PATH, XML,
            envelope("processRentalClaim", "rentalRequest", "<a/>"),
            "ServiceAuthorization", APIM_TOKEN, "X-Organisation-Id", "ORG123");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("processRentalClaimResponse");
    }

    @Test
    void shouldRejectApimWithoutOrganisation() throws Exception {
        assertThat(post(port, SERVICE_PATH, XML, envelope("processRentalClaim", "rentalRequest", "<a/>"),
            "ServiceAuthorization", APIM_TOKEN).statusCode()).isEqualTo(403);
    }

    @Test
    void shouldRejectOtherServiceOnBulkPath() throws Exception {
        assertThat(post(port, SERVICE_PATH, XML, envelope("processRentalClaim", "rentalRequest", "<a/>"),
            "ServiceAuthorization", XUI_TOKEN, "X-Organisation-Id", "ORG123").statusCode()).isEqualTo(403);
    }

    @Test
    void shouldRejectApimOnOtherPaths() throws Exception {
        assertThat(post(port, "/payment-update", "application/json", "{}",
            "ServiceAuthorization", APIM_TOKEN, "X-Organisation-Id", "ORG123").statusCode()).isEqualTo(403);
        assertThat(post(port, "/ccd-persistence/cases", "application/json", "{}",
            "ServiceAuthorization", APIM_TOKEN, "X-Organisation-Id", "ORG123").statusCode()).isEqualTo(403);
    }
}
