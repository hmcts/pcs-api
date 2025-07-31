package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;

class CrossBorderPostcodeSelectionTest extends BasePageTest {

    private EligibilityService eligibilityService;
    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        eligibilityService = mock(EligibilityService.class);
        event = buildPageInTestEvent(new CrossBorderPostcodeSelection(eligibilityService));
    }

    @ParameterizedTest
    @MethodSource("showConditionScenarios")
    void shouldSetShowStartTheServicePageBasedOnEligibility(
        String postcode,
        LegislativeCountry selectedCountry,
        EligibilityStatus status,
        YesOrNo expectedShowStartTheServicePage
    ) {
        // Arrange
        CaseDetails<PCSCase, State> caseDetails = new CaseDetails<>();
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder().postCode(postcode).build())
            .crossBorderCountriesList(DynamicStringList.builder()
                                          .value(DynamicStringListElement.builder()
                                                     .code(selectedCountry.name())
                                                     .label(selectedCountry.getLabel())
                                                     .build())
                                          .build())
            .build();
        caseDetails.setData(caseData);

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(status)
            .build();

        when(eligibilityService.checkEligibility(postcode, selectedCountry)).thenReturn(eligibilityResult);

        // Act
        AboutToStartOrSubmitResponse<PCSCase, State> response = getMidEventForPage(event, "crossBorderPostcodeSelection")
            .handle(caseDetails, null);

        // Assert
        assertThat(response.getData().getShowStartTheServicePage()).isEqualTo(expectedShowStartTheServicePage);
    }

    private static Stream<Arguments> showConditionScenarios() {
        return Stream.of(
            // Eligible property - should show start service page
            arguments(
                "TD9 0TU",
                LegislativeCountry.ENGLAND,
                EligibilityStatus.ELIGIBLE,
                YES
            ),
            // Not eligible property - should not show start service page
            arguments(
                "TD9 0TU",
                LegislativeCountry.SCOTLAND,
                EligibilityStatus.NOT_ELIGIBLE,
                NO
            ),
            // No match found - should not show start service page
            arguments(
                "LL65 1AA",
                LegislativeCountry.WALES,
                EligibilityStatus.NO_MATCH_FOUND,
                NO
            )
        );
    }
}
