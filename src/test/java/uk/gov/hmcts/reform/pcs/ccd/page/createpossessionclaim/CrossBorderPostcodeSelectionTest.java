package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrossBorderPostcodeSelectionTest extends BasePageTest {

    private static final String SOME_POSTCODE = "TX1 1TX";

    @Mock
    private EligibilityService eligibilityService;

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(new CrossBorderPostcodeSelection(eligibilityService));
    }

    @ParameterizedTest
    @MethodSource("eligibilityScenarios")
    void shouldLogEligibilityBasedOnCountrySelection(
        String postcode,
        LegislativeCountry selectedCountry,
        EligibilityStatus status
    ) {
        // Given
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

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response =
            getMidEventForPage(event, "crossBorderPostcodeSelection")
                .handle(caseDetails, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
    }

    private static Stream<Arguments> eligibilityScenarios() {
        return Stream.of(
            // Eligible property
            arguments(
                SOME_POSTCODE,
                LegislativeCountry.ENGLAND,
                EligibilityStatus.ELIGIBLE
            ),
            // Not eligible property
            arguments(
                SOME_POSTCODE,
                LegislativeCountry.SCOTLAND,
                EligibilityStatus.NOT_ELIGIBLE
            ),
            // No match found
            arguments(
                SOME_POSTCODE,
                LegislativeCountry.WALES,
                EligibilityStatus.NO_MATCH_FOUND
            )
        );
    }
}
