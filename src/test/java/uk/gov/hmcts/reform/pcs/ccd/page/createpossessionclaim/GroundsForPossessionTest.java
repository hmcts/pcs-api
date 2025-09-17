package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GroundsForPossessionTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new GroundsForPossession());
    }

    @Test
    void shouldClearGroundsOptionsWhenGroundsForPossessionIsYes() {
        // Given: One mandatory and one discretionary is selected
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsDiscretionaryGroundsOptions(Set.of(NoRentArrearsDiscretionaryGrounds.DOMESTIC_VIOLENCE))
            .noRentArrearsMandatoryGroundsOptions(Set.of(NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR))
            .groundsForPossession(YesOrNo.YES)
            .build();

        caseDetails.setData(caseData);

        // When: Mid event is executed
        callMidEventHandler(caseData);

        // Then: Sets should be cleared
        assertThat(caseDetails.getData().getNoRentArrearsMandatoryGroundsOptions()).isEmpty();
        assertThat(caseDetails.getData().getNoRentArrearsDiscretionaryGroundsOptions()).isEmpty();

    }

    @Test
    void shouldNotClearGroundsOptionsWhenGroundsForPossessionIsNo() {
        // Given: One mandatory and one discretionary is selected
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsDiscretionaryGroundsOptions(Set.of(NoRentArrearsDiscretionaryGrounds.DOMESTIC_VIOLENCE))
            .noRentArrearsMandatoryGroundsOptions(Set.of(NoRentArrearsMandatoryGrounds.ANTISOCIAL_BEHAVIOUR))
            .groundsForPossession(YesOrNo.NO)
            .build();

        caseDetails.setData(caseData);

        // When: Mid event is executed
        callMidEventHandler(caseData);


        // Then: Sets should not be cleared
        assertThat(caseDetails.getData().getNoRentArrearsMandatoryGroundsOptions()).isNotEmpty();
        assertThat(caseDetails.getData().getNoRentArrearsDiscretionaryGroundsOptions()).isNotEmpty();
    }
}
