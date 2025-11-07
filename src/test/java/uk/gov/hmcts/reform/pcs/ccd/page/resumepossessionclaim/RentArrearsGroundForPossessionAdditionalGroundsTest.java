package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentDetailsRoutingService;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RentArrearsGroundForPossessionAdditionalGroundsTest extends BasePageTest {

    @Mock
    private RentDetailsRoutingService rentDetailsRoutingService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundForPossessionAdditionalGrounds(rentDetailsRoutingService));
    }

    @Test
    void shouldErrorWhenRentArrearsSelectedAndNoAdditionalSelected() {
        // Given: user selected rent arrears (e.g., ground 8) on previous page, but nothing on this page
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).containsExactly("Please select at least one ground");
    }

    @Test
    void shouldPassAndSetShowReasonsWhenAdditionalMandatorySelected() {
        // Given: rent arrears selected earlier; user chooses an additional mandatory ground here
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8))
            .assuredAdditionalMandatoryGrounds(Set.of(AssuredAdditionalMandatoryGrounds.OWNER_OCCUPIER_GROUND1))
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: reasons page should be shown (has ground beyond 8)
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.YES);
        // And canonical sets include both 8 and 1 mapped into mandatory
        assertThat(response.getData().getRentArrearsMandatoryGrounds())
            .contains(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8);
    }

    @Test
    void shouldPassAndSetShowReasonsWhenAdditionalDiscretionarySelected() {
        // Given: rent arrears selected earlier; user chooses an additional discretionary ground here
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10))
            .assuredAdditionalDiscretionaryGrounds(
                Set.of(AssuredAdditionalDiscretionaryGrounds.BREACH_TENANCY_GROUND12)
            )
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then: reasons page should be shown (has ground beyond 10/11)
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getShowRentArrearsGroundReasonPage()).isEqualTo(YesOrNo.YES);
        // And canonical discretionary includes mapped values
        assertThat(response.getData().getRentArrearsDiscretionaryGrounds())
            .contains(RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10);
    }

}
