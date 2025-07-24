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
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.COMMUNITY_LANDLORD;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.MORTGAGE_PROVIDER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.OTHER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.PRIVATE_LANDLORD;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.PROVIDER_OF_SOCIAL_HOUSING;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

class SelectClaimantTypeTest extends BasePageTest {

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new SelectClaimantType());
    }

    @ParameterizedTest
    @MethodSource("claimantTypeScenarios")
    void shouldSetDisplayFlagsInMidEventCallback(LegislativeCountry legislativeCountry,
                                                 ClaimantType claimantType,
                                                 YesOrNo showNotEligibleEngland,
                                                 YesOrNo showNotEligibleWales) {
        // Given
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        DynamicStringList claimantTypeSelection = DynamicStringList.builder()
            .value(DynamicStringListElement.builder().code(claimantType.name()).build())
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(legislativeCountry.getLabel())
            .claimantType(claimantTypeSelection)
            .build();

        caseDetails.setData(caseData);

        // When
        MidEvent<PCSCase, State> midEvent = getMidEventForPage(event, "selectClaimantType");
        midEvent.handle(caseDetails, null);

        // Then
        assertThat(caseData.getShowClaimantTypeNotEligibleEngland()).isEqualTo(showNotEligibleEngland);
        assertThat(caseData.getShowClaimantTypeNotEligibleWales()).isEqualTo(showNotEligibleWales);
    }

    private static Stream<Arguments> claimantTypeScenarios() {

        return Stream.of(
            // Country, claimant type, show England ineligible page, show Wales ineligble page
            arguments(ENGLAND, PROVIDER_OF_SOCIAL_HOUSING, NO, NO),
            arguments(WALES, COMMUNITY_LANDLORD, NO, NO),

            arguments(ENGLAND, PRIVATE_LANDLORD, YES, NO),
            arguments(ENGLAND, MORTGAGE_PROVIDER, YES, NO),
            arguments(ENGLAND, OTHER, YES, NO),
            arguments(WALES, PRIVATE_LANDLORD, NO, YES),
            arguments(WALES, MORTGAGE_PROVIDER, NO, YES),
            arguments(WALES, OTHER, NO, YES)
        );

    }


}
