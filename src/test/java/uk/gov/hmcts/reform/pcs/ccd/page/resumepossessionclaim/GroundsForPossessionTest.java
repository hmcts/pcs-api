package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredDiscretionaryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredMandatoryGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsGroundsOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GroundsForPossessionTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new GroundsForPossession());
    }

    @Test
    void shouldClearGroundsOptionsWhenGroundsForPossessionIsYes() {
        // Given: One mandatory and one discretionary is selected
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsGroundsOptions(
                NoRentArrearsGroundsOptions.builder()
                    .mandatoryGrounds(Set.of(AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A))
                    .discretionaryGrounds(
                        Set.of(AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A))
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.YES)
            .build();

        caseDetails.setData(caseData);

        // When: Mid event is executed
        callMidEventHandler(caseData);

        // Then: Sets should be cleared
        assertThat(caseDetails.getData().getNoRentArrearsGroundsOptions()
                       .getMandatoryGrounds()).isEmpty();
        assertThat(caseDetails.getData().getNoRentArrearsGroundsOptions()
                       .getDiscretionaryGrounds()).isEmpty();

    }

    @Test
    void shouldNotClearGroundsOptionsWhenGroundsForPossessionIsNo() {
        // Given: One mandatory and one discretionary is selected
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .noRentArrearsGroundsOptions(
                NoRentArrearsGroundsOptions.builder()
                    .mandatoryGrounds(Set.of(AssuredMandatoryGround.ANTISOCIAL_BEHAVIOUR_GROUND7A))
                    .discretionaryGrounds(
                        Set.of(AssuredDiscretionaryGround.DOMESTIC_VIOLENCE_GROUND14A))
                    .build()
            )
            .claimDueToRentArrears(YesOrNo.NO)
            .build();

        caseDetails.setData(caseData);

        // When: Mid event is executed
        callMidEventHandler(caseData);


        // Then: Sets should not be cleared
        assertThat(caseDetails.getData().getNoRentArrearsGroundsOptions()
                       .getMandatoryGrounds()).isNotEmpty();
        assertThat(caseDetails.getData().getNoRentArrearsGroundsOptions()
                       .getDiscretionaryGrounds()).isNotEmpty();
    }
}
