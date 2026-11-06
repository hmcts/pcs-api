package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenAppHoldingPage tests")
class GenAppHoldingPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new GenAppHoldingPage());
    }

    @Test
    @DisplayName("Should block progression from Gen App holding page")
    void shouldBlockProgression() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().warrantDetails(WarrantDetails.builder()
                .showChangeNameAddressPage(YesOrNo.YES)
                .build())
              .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response =
            callMidEventHandler(caseData);


        assertThat(response.getErrorMessageOverride())
            .isEqualTo("You cannot continue with this application");
    }

}
