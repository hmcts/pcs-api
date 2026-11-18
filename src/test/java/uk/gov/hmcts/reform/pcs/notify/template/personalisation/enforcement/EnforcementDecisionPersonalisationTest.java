package uk.gov.hmcts.reform.pcs.notify.template.personalisation.enforcement;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.TemplatePersonalisation;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnforcementDecisionPersonalisationTest {

    @Test
    void shouldGeneratePersonalisationMap() {
        // Given
        Map<String, Object> baseMap = Map.of(
            "baseKey1", "baseValue1",
            "baseKey2", "baseValue2"
        );

        String expectedCaseOverviewUrl = "case overview URL";
        String expectedRejectionDetails = "rejection details";

        TemplatePersonalisation basePersonalisation = mock(TemplatePersonalisation.class);
        when(basePersonalisation.toMap()).thenReturn(baseMap);

        EnforcementDecisionPersonalisation personalisation = EnforcementDecisionPersonalisation.builder()
            .base(basePersonalisation)
            .caseOverviewUrl(expectedCaseOverviewUrl)
            .rejectionDetails(expectedRejectionDetails)
            .build();

        // When
        Map<String, Object> personalisationMap = personalisation.toMap();

        // Then
        assertThat(personalisationMap)
            .containsAllEntriesOf(baseMap)
            .contains(
                entry("caseOverviewUrl", expectedCaseOverviewUrl),
                entry("rejectionDetails", expectedRejectionDetails)
            );
    }
}
