package uk.gov.hmcts.reform.pcs.bulkclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "security.anonymousPaths[0]=/health")
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DisplayName("Bulk claims APIM S2S lock integration tests")
class BulkClaimApimSecurityIT extends AbstractPostgresContainerIT {

    private static final String APIM_TOKEN = "Bearer apim-token";
    private static final String XUI_TOKEN = "Bearer xui-token";

    @Autowired
    private MockMvc mockMvc;

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
    void shouldReachControllerForApimWithOrganisation() throws Exception {
        mockMvc.perform(post("/v2/bulk-claims/rental")
                .header("ServiceAuthorization", APIM_TOKEN)
                .header("X-Organisation-Id", "ORG123")
                .contentType(MediaType.APPLICATION_XML)
                .content("<a/>"))
            .andExpect(status().isNotImplemented());
    }

    @Test
    void shouldRejectApimWithoutOrganisation() throws Exception {
        mockMvc.perform(post("/v2/bulk-claims/rental")
                .header("ServiceAuthorization", APIM_TOKEN)
                .contentType(MediaType.APPLICATION_XML)
                .content("<a/>"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectOtherServiceOnBulkPath() throws Exception {
        mockMvc.perform(post("/v2/bulk-claims/rental")
                .header("ServiceAuthorization", XUI_TOKEN)
                .header("X-Organisation-Id", "ORG123")
                .contentType(MediaType.APPLICATION_XML)
                .content("<a/>"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectApimOnOtherPaths() throws Exception {
        mockMvc.perform(post("/payment-update")
                .header("ServiceAuthorization", APIM_TOKEN)
                .header("X-Organisation-Id", "ORG123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/ccd-persistence/cases")
                .header("ServiceAuthorization", APIM_TOKEN)
                .header("X-Organisation-Id", "ORG123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
}
