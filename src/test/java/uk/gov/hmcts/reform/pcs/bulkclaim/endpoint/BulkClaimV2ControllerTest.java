package uk.gov.hmcts.reform.pcs.bulkclaim.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.security.BulkClaimApimS2sFilter;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimV2Handler;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.NotImplementedBulkClaimV2Handler;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BulkClaimV2ControllerTest {

    @Mock
    private FeatureToggleService featureToggleService;

    private BulkClaimProperties properties;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        properties = new BulkClaimProperties();
        properties.setMaxRequestSize(DataSize.ofBytes(16));
        BulkClaimV2Handler handler = new NotImplementedBulkClaimV2Handler();
        mockMvc = MockMvcBuilders
            .standaloneSetup(new BulkClaimV2Controller(featureToggleService, properties, handler))
            .build();
    }

    @Test
    void shouldReturnNotFoundForAllEndpointsWhenFlagOff() throws Exception {
        mockMvc.perform(post("/v2/bulk-claims/rental").contentType(MediaType.APPLICATION_XML).content("<a/>"))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/v2/bulk-claims/mortgage").contentType(MediaType.APPLICATION_XML).content("<a/>"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/v2/bulk-claims/claims/abc")).andExpect(status().isNotFound());
        mockMvc.perform(get("/v2/bulk-claims/schema/x.xsd")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForMortgageWhenMortgageFlagOff() throws Exception {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);

        mockMvc.perform(post("/v2/bulk-claims/mortgage").contentType(MediaType.APPLICATION_XML).content("<a/>"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnPayloadTooLargeWhenBodyExceedsLimit() throws Exception {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);

        mockMvc.perform(post("/v2/bulk-claims/rental").contentType(MediaType.APPLICATION_XML)
                .content("<a>0123456789012345</a>"))
            .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void shouldReturnNotImplementedWhenEnabled() throws Exception {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);
        lenient().when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2_MORTGAGE)).thenReturn(true);

        mockMvc.perform(post("/v2/bulk-claims/rental").contentType(MediaType.TEXT_XML).content("<a/>")
                .requestAttr(BulkClaimApimS2sFilter.ORGANISATION_ID_ATTRIBUTE, "ORG1"))
            .andExpect(status().isNotImplemented());
        mockMvc.perform(post("/v2/bulk-claims/mortgage").contentType(MediaType.APPLICATION_XML).content("<a/>")
                .param("validateOnly", "true"))
            .andExpect(status().isNotImplemented());
        mockMvc.perform(get("/v2/bulk-claims/claims/abc")).andExpect(status().isNotImplemented());
        mockMvc.perform(get("/v2/bulk-claims/schema/x.xsd")).andExpect(status().isNotImplemented());
    }
}
