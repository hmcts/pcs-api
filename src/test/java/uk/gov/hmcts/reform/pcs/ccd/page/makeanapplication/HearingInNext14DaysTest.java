package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.FeatureFlags;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class HearingInNext14DaysTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new HearingInNext14Days());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "YES, YES, YES",
        "YES, NO, YES",
        "NO, YES, YES",
        "NO, NO, NO",
        "null, YES, YES",
        "null, NO, NO"
    }, nullValues = "null")
    void shouldShowHwfScreensBasedOnRelease1bAndHearingDate(VerticalYesNo release1bEnabled,
                                                          VerticalYesNo within14Days,
                                                          VerticalYesNo expectedShowHwfScreens) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .featureFlags(FeatureFlags.builder().release1bEnabled(release1bEnabled).build())
            .xuiGenAppRequest(XuiGenAppRequest.builder()
                                  .within14Days(within14Days)
                                  .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        PCSCase updatedCaseData = response.getData();
        assertThat(updatedCaseData.getXuiGenAppRequest().getShowHwfScreens()).isEqualTo(expectedShowHwfScreens);
    }

    @ParameterizedTest
    @EnumSource(VerticalYesNo.class)
    void shouldUseProductionHwfRoutingWhenFeatureFlagsAreMissing(VerticalYesNo within14Days) {
        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder().within14Days(within14Days).build())
            .build();

        PCSCase updatedCaseData = callMidEventHandler(caseData).getData();

        assertThat(updatedCaseData.getXuiGenAppRequest().getShowHwfScreens()).isEqualTo(within14Days);
    }

    @Test
    void shouldOnlyShowOtherPartiesExplanationWhenRelease1bEnabled() {
        assertThat(event.getFields().getFields().stream().map(Field.FieldBuilder::build).toList())
            .filteredOn(field -> "hearingInNext14Days-info".equals(field.getId()))
            .singleElement()
            .satisfies(field -> assertThat(field.getShowCondition())
                .isEqualTo("featureFlags.release1bEnabled=\"YES\""));
    }

}
