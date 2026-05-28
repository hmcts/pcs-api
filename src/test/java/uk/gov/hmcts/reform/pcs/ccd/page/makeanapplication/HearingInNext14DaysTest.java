package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
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
    @EnumSource(value = VerticalYesNo.class)
    void shouldSetShowHwfScreens(VerticalYesNo hearingWithinNext14Days) {
        // Given
        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder()
                                  .within14Days(hearingWithinNext14Days)
                                  .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        PCSCase updatedCaseData = response.getData();
        assertThat(updatedCaseData.getXuiGenAppRequest().getShowHwfScreens()).isEqualTo(hearingWithinNext14Days);
    }

}
