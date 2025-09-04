package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.MidEvent;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

class SelectClaimTypeTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new SelectClaimType());
    }

    @ParameterizedTest
    @MethodSource("claimTypeScenarios")
    void shouldSetDisplayFlagsInMidEventCallback(LegislativeCountry legislativeCountry,
                                                 VerticalYesNo isClaimAgainstTrespassers,
                                                 YesOrNo showNotEligibleEngland,
                                                 YesOrNo showNotEligibleWales) {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(legislativeCountry)
            .claimAgainstTrespassers(isClaimAgainstTrespassers)
            .build();

        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "selectClaimType");
        midEvent.handle(caseDetails, null);

        // Then
        assertThat(caseData.getShowClaimTypeNotEligibleEngland()).isEqualTo(showNotEligibleEngland);
        assertThat(caseData.getShowClaimTypeNotEligibleWales()).isEqualTo(showNotEligibleWales);
    }

    private static Stream<Arguments> claimTypeScenarios() {

        return Stream.of(
            // Country, Claim is against trespassers, show England ineligible page, show Wales ineligble page
            arguments(ENGLAND, VerticalYesNo.NO, NO, NO),
            arguments(WALES, VerticalYesNo.NO, NO, NO),

            arguments(ENGLAND, VerticalYesNo.YES, YES, NO),
            arguments(WALES, VerticalYesNo.YES, NO, YES)
        );

    }

}
