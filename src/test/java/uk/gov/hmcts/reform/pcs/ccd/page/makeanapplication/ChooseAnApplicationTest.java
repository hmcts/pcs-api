package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.DefendantGenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseAnApplicationTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ChooseAnApplication());
    }

    @Test
    void shouldCopyDefendantGenAppTypeToGenAppType() {
        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder()
                                  .defendantGenAppType(DefendantGenAppType.SET_ASIDE)
                                  .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getData().getXuiGenAppRequest().getApplicationType()).isEqualTo(GenAppType.SET_ASIDE);
    }
}
