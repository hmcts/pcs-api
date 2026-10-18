package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The warrant return fields are unwrapped into the case data with a prefix, so their JSON names have to match
 * the CCD field ids the SDK generates: prefix + UpperCamelCase field name.
 */
class WarrantReturnJsonTest {

    private final ObjectMapper mapper = new JacksonConfiguration().getMapper();

    @Test
    void shouldReadTheRecordWarrantReturnAnswersFromTheCcdFieldIds() throws Exception {
        String json = """
            {
              "warrantReturnType": "FINAL",
              "warrantReturnOutcome": "EVICTION_UNSUCCESSFUL",
              "warrantReturnUnsuccessfulReturnName": "WARRANT_SUSPENDED_BY_ORDER",
              "warrantReturnAdditionalDetails": "Some details"
            }
            """;

        WarrantReturnDetails warrantReturn = mapper.readValue(json, PCSCase.class)
            .getEnforcementOrder().getWarrantReturn();

        assertThat(warrantReturn.getType()).isEqualTo(WarrantReturnType.FINAL);
        assertThat(warrantReturn.getOutcome()).isEqualTo(WarrantReturnOutcome.EVICTION_UNSUCCESSFUL);
        assertThat(warrantReturn.getUnsuccessfulReturnName())
            .isEqualTo(UnsuccessfulWarrantReturnName.WARRANT_SUSPENDED_BY_ORDER);
        assertThat(warrantReturn.getAdditionalDetails()).isEqualTo("Some details");
    }

    @Test
    void shouldWriteTheEvictionDetailsMarkupUnderItsCcdFieldId() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .warrantReturn(WarrantReturnDetails.builder().evictionDetailsMarkup("<table/>").build())
                .build())
            .build();

        Map<String, Object> json = mapper.convertValue(caseData, new TypeReference<>() { });

        assertThat(json).containsEntry("warrantReturnEvictionDetailsMarkup", "<table/>");
    }

    @Test
    void shouldWriteTheLatestWarrantReturnUnderItsCcdFieldIds() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .latestWarrantReturn(LatestWarrantReturn.builder()
                    .type(WarrantReturnType.FINAL)
                    .name("Warrant suspended by Order")
                    .outcome(WarrantReturnOutcome.EVICTION_UNSUCCESSFUL)
                    .additionalDetails("Some details")
                    .build())
                .build())
            .build();

        Map<String, Object> json = mapper.convertValue(caseData, new TypeReference<>() { });

        assertThat(json)
            .containsEntry("latestWarrantReturnType", "FINAL")
            .containsEntry("latestWarrantReturnName", "Warrant suspended by Order")
            .containsEntry("latestWarrantReturnOutcome", "EVICTION_UNSUCCESSFUL")
            .containsEntry("latestWarrantReturnAdditionalDetails", "Some details");
    }
}
