package uk.gov.hmcts.reform.pcs.bulkclaim.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.unit.DataSize;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.security.BulkClaimApimS2sFilter;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimV2Handler;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BulkClaimV2ControllerTest {

    private static final String ORGANISATION_ID = "ORG1";

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private BulkClaimV2Handler handler;

    private BulkClaimProperties properties;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        properties = new BulkClaimProperties();
        properties.setMaxRequestSize(DataSize.ofBytes(16));
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

        verifyNoInteractions(handler);
    }

    @Test
    void shouldReturnNotFoundForMortgageWhenMortgageFlagOff() throws Exception {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);

        mockMvc.perform(post("/v2/bulk-claims/mortgage").contentType(MediaType.APPLICATION_XML).content("<a/>"))
            .andExpect(status().isNotFound());

        verifyNoInteractions(handler);
    }

    @Test
    void shouldReturnPayloadTooLargeWhenBodyExceedsLimit() throws Exception {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);

        mockMvc.perform(post("/v2/bulk-claims/rental").contentType(MediaType.APPLICATION_XML)
                .content("<a>0123456789012345</a>"))
            .andExpect(status().isPayloadTooLarge());

        verify(handler, never()).submitRental(any(), any(), anyBoolean());
    }

    @Test
    void shouldReadBodyAndOrganisationIdIntoHandler() throws Exception {
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);
        byte[] body = "<a/>".getBytes(StandardCharsets.UTF_8);
        when(handler.submitRental(eq(body), eq(ORGANISATION_ID), eq(true)))
            .thenReturn(ResponseEntity.ok("<ok/>".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(post("/v2/bulk-claims/rental").contentType(MediaType.TEXT_XML).content(body)
                .param("validateOnly", "true")
                .requestAttr(BulkClaimApimS2sFilter.ORGANISATION_ID_ATTRIBUTE, ORGANISATION_ID))
            .andExpect(status().isOk())
            .andExpect(content().string("<ok/>"));
    }

    @Test
    void shouldRouteMortgageStatusAndSchemaToHandlerWhenEnabled() throws Exception {
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2)).thenReturn(true);
        when(featureToggleService.isEnabled(FeatureFlag.BULK_CLAIMS_V2_MORTGAGE)).thenReturn(true);
        when(handler.submitMortgage(any(), eq(ORGANISATION_ID), eq(false))).thenReturn(ResponseEntity.ok().build());
        when(handler.status(ORGANISATION_ID, "abc")).thenReturn(ResponseEntity.ok().build());
        when(handler.schema("x.xsd")).thenReturn(ResponseEntity.ok().build());

        mockMvc.perform(post("/v2/bulk-claims/mortgage").contentType(MediaType.APPLICATION_XML).content("<a/>")
                .requestAttr(BulkClaimApimS2sFilter.ORGANISATION_ID_ATTRIBUTE, ORGANISATION_ID))
            .andExpect(status().isOk());
        mockMvc.perform(get("/v2/bulk-claims/claims/abc")
                .requestAttr(BulkClaimApimS2sFilter.ORGANISATION_ID_ATTRIBUTE, ORGANISATION_ID))
            .andExpect(status().isOk());
        mockMvc.perform(get("/v2/bulk-claims/schema/x.xsd")).andExpect(status().isOk());

        assertThat(properties.getMaxRequestSize().toBytes()).isEqualTo(16);
    }
}
