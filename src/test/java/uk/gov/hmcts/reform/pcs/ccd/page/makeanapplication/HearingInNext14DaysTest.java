package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    void shouldShowHwfScreensWhenHearingIsNotWithinNext14Days() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder()
                                  .within14Days(VerticalYesNo.NO)
                                  .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        PCSCase updatedCaseData = response.getData();
        assertThat(updatedCaseData.getXuiGenAppRequest().getShowHwfScreens()).isEqualTo(VerticalYesNo.YES);
    }

}
