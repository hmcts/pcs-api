package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationFeeTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ApplicationFee());
    }

    @Test
    void shouldReturnErrorIfFeeReceivedIsNo() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder()
                                    .feeReceived(VerticalYesNo.NO)
                                    .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride())
            .isEqualTo("You must request payment from the applicant before entering this application");
    }

    @Test
    void shouldNotReturnErrorIfFeeReceivedIsYes() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .enterGenAppRequest(EnterGenAppRequest.builder()
                                    .feeReceived(VerticalYesNo.YES)
                                    .build())
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData()).isEqualTo(caseData);
        assertThat(response.getErrorMessageOverride()).isNull();
    }

}
