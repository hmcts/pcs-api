package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.COMMUNITY_LANDLORD;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.MORTGAGE_LENDER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.OTHER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.PRIVATE_LANDLORD;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.PROVIDER_OF_SOCIAL_HOUSING;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

@ExtendWith(MockitoExtension.class)
class SelectClaimantTypeTest extends BasePageTest {

    @Mock
    private UnsubmittedCaseDataService unsubmittedCaseDataService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new SelectClaimantType(unsubmittedCaseDataService));
    }

    @ParameterizedTest
    @MethodSource("claimantTypeScenarios")
    void shouldSetDisplayFlagsInMidEventCallback(LegislativeCountry legislativeCountry,
                                                 ClaimantType claimantType,
                                                 YesOrNo showNotEligibleEngland,
                                                 YesOrNo showNotEligibleWales) {
        // Given
        DynamicStringList claimantTypeSelection = createClaimantTypeSelection(claimantType);

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(legislativeCountry)
            .claimantType(claimantTypeSelection)
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowClaimantTypeNotEligibleEngland()).isEqualTo(showNotEligibleEngland);
        assertThat(caseData.getShowClaimantTypeNotEligibleWales()).isEqualTo(showNotEligibleWales);
    }

    @Test
    void shouldRemoveDraftDataIfUserChoseNotToResume() {
        // Given
        DynamicStringList claimantTypeSelection = createClaimantTypeSelection(PROVIDER_OF_SOCIAL_HOUSING);

        PCSCase caseData = PCSCase.builder()
            .claimantType(claimantTypeSelection)
            .hasUnsubmittedCaseData(YES)
            .resumeClaimKeepAnswers(NO)
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        verify(unsubmittedCaseDataService).deleteUnsubmittedCaseData(TEST_CASE_REFERENCE);
        assertThat(caseData.getHasUnsubmittedCaseData()).isEqualTo(NO);
    }

    private static Stream<Arguments> claimantTypeScenarios() {

        return Stream.of(
            // Country, claimant type, show England ineligible page, show Wales ineligble page
            arguments(ENGLAND, PROVIDER_OF_SOCIAL_HOUSING, NO, NO),
            arguments(WALES, COMMUNITY_LANDLORD, NO, NO),

            arguments(ENGLAND, PRIVATE_LANDLORD, YES, NO),
            arguments(ENGLAND, MORTGAGE_LENDER, YES, NO),
            arguments(ENGLAND, OTHER, YES, NO),
            arguments(WALES, PRIVATE_LANDLORD, NO, YES),
            arguments(WALES, MORTGAGE_LENDER, NO, YES),
            arguments(WALES, OTHER, NO, YES)
        );

    }

    private static DynamicStringList createClaimantTypeSelection(ClaimantType claimantType) {
        return DynamicStringList.builder()
            .value(DynamicStringListElement.builder().code(claimantType.name()).build())
            .build();
    }

}
