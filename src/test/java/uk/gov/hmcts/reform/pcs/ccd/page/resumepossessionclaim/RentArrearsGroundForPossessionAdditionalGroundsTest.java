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
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentDetailsRoutingService;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentArrearsGroundForPossessionAdditionalGroundsTest extends BasePageTest {

    @Mock
    private RentDetailsRoutingService rentDetailsRoutingService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsGroundForPossessionAdditionalGrounds(rentDetailsRoutingService));
        // Default mock behavior: return YES when rent arrears grounds are present
        when(rentDetailsRoutingService.shouldShowRentDetails(any(PCSCase.class)))
            .thenAnswer(invocation -> {
                PCSCase caseData = invocation.getArgument(0);
                Set<RentArrearsMandatoryGrounds> mandatory = caseData.getRentArrearsMandatoryGrounds();
                Set<RentArrearsDiscretionaryGrounds> discretionary = caseData.getRentArrearsDiscretionaryGrounds();
                boolean hasRentGrounds = (mandatory != null
                    && mandatory.contains(RentArrearsMandatoryGrounds.SERIOUS_RENT_ARREARS_GROUND8))
                    || (discretionary != null && (
                        discretionary.contains(RentArrearsDiscretionaryGrounds.RENT_ARREARS_GROUND10)
                        || discretionary.contains(RentArrearsDiscretionaryGrounds.PERSISTENT_DELAY_GROUND11)
                    ));
                return YesOrNo.from(hasRentGrounds);
            });
    }

    @ParameterizedTest
    @MethodSource("provideNoRentArrearsScenarios")
    void shouldSetCorrectShowFlagForReasonPage(
        Set<RentArrearsGround> rentArrearsGrounds,
        Set<RentArrearsMandatoryGrounds> mandatoryGrounds,
        Set<RentArrearsDiscretionaryGrounds> discretionaryGrounds,
        YesOrNo expectedShowFlag) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .rentArrearsGrounds(rentArrearsGrounds)
            .rentArrearsMandatoryGrounds(mandatoryGrounds)
            .rentArrearsDiscretionaryGrounds(discretionaryGrounds)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        PCSCase updatedCaseData = response.getData();

        assertThat(updatedCaseData.getShowRentArrearsGroundReasonPage()).isEqualTo(expectedShowFlag);
    }

    private static Stream<Arguments> provideNoRentArrearsScenarios() {
        return Stream.of(
            Arguments.of(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10,
                                RentArrearsGround.PERSISTENT_DELAY_GROUND11),
                         Set.of(),
                         Set.of(),
                         YesOrNo.NO),
            Arguments.of(Set.of(RentArrearsGround.RENT_ARREARS_GROUND10,
                                RentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8,
                                RentArrearsGround.PERSISTENT_DELAY_GROUND11),
                         Set.of(),
                         Set.of(),
                         YesOrNo.NO),
            Arguments.of(Set.of(),
                         Set.of(RentArrearsMandatoryGrounds.REDEVELOPMENT_GROUND6),
                         Set.of(),
                         YesOrNo.YES),
            Arguments.of(Set.of(),
                         Set.of(),
                         Set.of(RentArrearsDiscretionaryGrounds.BREACH_TENANCY_GROUND12),
                         YesOrNo.YES)
        );
    }
}
