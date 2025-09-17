package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsOrBreachOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class RentArrearsOrBreachOfTenancyGroundTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RentArrearsOrBreachOfTenancyGround());
    }

    @ParameterizedTest
    @MethodSource("midEventScenarios")
    void shouldSetDisplayFlagsInMidEventCallback(
        Set<RentArrearsOrBreachOfTenancy> rentAreasOrBreach,
        YesOrNo expectedShowBreachOfTenancyTextarea) {

        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .rentArrearsOrBreachOfTenancy(rentAreasOrBreach)
            .build();
        caseDetails.setData(caseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowBreachOfTenancyTextarea()).isEqualTo(expectedShowBreachOfTenancyTextarea);
    }

    private static Stream<Arguments> midEventScenarios() {
        return Stream.of(
            arguments(
                Set.of(RentArrearsOrBreachOfTenancy.BREACH_OF_TENANCY),
                YesOrNo.YES
            ),
            arguments(
                Set.of(RentArrearsOrBreachOfTenancy.RENT_ARREARS),
                YesOrNo.NO
            ),
            arguments(
                Set.of(),
                YesOrNo.NO
            )
        );
    }


}
