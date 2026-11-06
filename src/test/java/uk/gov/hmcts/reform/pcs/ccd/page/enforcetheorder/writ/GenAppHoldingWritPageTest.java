package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenAppHoldingWritPage tests")
class GenAppHoldingWritPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new GenAppHoldingWritPage());
    }

    @Test
    @DisplayName("Should block progression from Gen App holding page")
    void shouldBlockProgression() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
              .writDetails(WritDetails.builder()
               .showChangeNameAddressPage(YesOrNo.YES)
               .build())
              .build())
            .build();

        AboutToStartOrSubmitResponse<PCSCase, State> response =
            callMidEventHandler(caseData);

        assertThat(response.getErrorMessageOverride())
            .isEqualTo(
                "You cannot continue with this application");
    }
}
