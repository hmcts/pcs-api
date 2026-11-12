package uk.gov.hmcts.reform.pcs.ccd.page.makeanapplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.XuiGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseAnApplicationTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new ChooseAnApplication());
    }

    @Test
    void shouldSetApplicationTypeAndShowNoticeScreensToNoWhenTypeIsSuspend() {
        DynamicStringListElement genAppType = DynamicStringListElement.builder()
            .code(GenAppType.ADJOURN.name())
            .build();

        DynamicStringList optionList = DynamicStringList.builder()
            .value(genAppType)
            .build();

        PCSCase caseData = PCSCase.builder()
            .xuiGenAppRequest(XuiGenAppRequest.builder()
                                  .applicationTypeOptionList(optionList)
                                  .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getData().getXuiGenAppRequest().getApplicationType()).isEqualTo(GenAppType.ADJOURN);
        assertThat(response.getData().getXuiGenAppRequest().getShowNoticeScreens()).isEqualTo(VerticalYesNo.YES);
    }
}
