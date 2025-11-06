package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentDetailsRoutingService;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RentArrearsGroundForPossessionAdditionalGroundsTest extends BasePageTest {

    @Mock
    private RentDetailsRoutingService rentDetailsRoutingService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundForPossessionAdditionalGrounds(rentDetailsRoutingService));
    }

    @ParameterizedTest
    @MethodSource("provideNoRentArrearsScenarios")
    void shouldSetShowFlagForReasonPageOrThrowMidEventError(
        Set<RentArrearsMandatoryGrounds> mandatoryGrounds,
        Set<RentArrearsDiscretionaryGrounds> discretionaryGrounds,
        YesOrNo expectedShowFlag,
        boolean errorExpected) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsMandatoryGrounds(mandatoryGrounds)
            .rentArrearsDiscretionaryGrounds(discretionaryGrounds)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);
        PCSCase updatedCaseData = response.getData();

        // Then
        if (errorExpected) {
            assertThat(response.getErrors()).containsExactly("Please select at least one ground");
        } else {
            assertThat(updatedCaseData.getShowRentArrearsGroundReasonPage()).isEqualTo(expectedShowFlag);
        }
    }

    private static Stream<Arguments> provideNoRentArrearsScenarios() {
        return Stream.of(
            //No grounds provided
            Arguments.of(Set.of(),
                         Set.of(),
                         YesOrNo.NO,
                         true),

            // Mandatory Rent arrears ground + Other ground
            Arguments.of(Set.of(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8,
                                RentArrearsMandatoryGrounds.REDEVELOPMENT_GROUND6),
                         Set.of(),
                         YesOrNo.YES,
                         false),

            // Only Mandatory Rent arrears ground
            Arguments.of(Set.of(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8),
                         Set.of(),
                         YesOrNo.NO,
                         false),

            // Discretionary Rent arrears ground + Other ground
            Arguments.of(Set.of(),
                         Set.of(RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10,
                                RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12),
                         YesOrNo.YES,
                         false),

            // Only Discretionary Rent arrears ground
            Arguments.of(Set.of(),
                         Set.of(RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11),
                         YesOrNo.NO,
                         false),

            // Only Mandatory & Discretionary Rent arrears grounds
            Arguments.of(Set.of(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8),
                         Set.of(RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11),
                         YesOrNo.NO,
                         false)
        );
    }

}
