package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrossBorderPostcodeSelectionTest extends BasePageTest {

    private static final String SOME_POSTCODE = "TX1 1TX";

    @Mock
    private EligibilityService eligibilityService;

    private Event<PCSCase, UserRole, State> event;

    @BeforeEach
    void setUp() {
        event = buildPageInTestEvent(
            new CrossBorderPostcodeSelection(eligibilityService)
        );
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
                                          .value(DynamicStringListElement
                                                     .builder()
                                                     .code(selectedCountry
                                                               .name())
                                                     .label(selectedCountry
                                                                .getLabel())
                                                     .build())
                                          .build())
            .build();
        caseDetails.setData(caseData);

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(status)
            .build();

        when(eligibilityService.checkEligibility(postcode, selectedCountry))
            .thenReturn(eligibilityResult);

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


    @ParameterizedTest
    @MethodSource("eligibleCountries")
    @DisplayName("ELIGIBLE keeps normal flow and cross-border page visible")
    void shouldContinueToClaimantInfoPageWhenCrossBorderPropertyIsEligible(
        LegislativeCountry selectedCountry
    ) {
        // Given: page visible (from MakeAClaim), PNE hidden
        var caseDetails = buildCrossBorderCaseWithFlags(
            SOME_POSTCODE,
            selectedCountry,
            YesOrNo.YES,
            YesOrNo.NO
        );

        var result = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .build();

        when(eligibilityService.checkEligibility(SOME_POSTCODE, selectedCountry))
            .thenReturn(result);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> resp =
            getMidEventForPage(event, "crossBorderPostcodeSelection")
                .handle(caseDetails, null);

        // Then
        var data = resp.getData();
        assertThat(data.getShowPropertyNotEligiblePage()).isEqualTo(YesOrNo.NO);
        assertThat(data.getShowCrossBorderPage()).isEqualTo(YesOrNo.YES);
    }

    @ParameterizedTest
    @MethodSource("eligibleCountries")
    @DisplayName("NOT_ELIGIBLE shows PropertyNotEligible and keeps cross-border page visible")
    void shouldShowPropertyNotEligiblePageWhenCrossBorderPropertyIsNotEligible(
        LegislativeCountry selectedCountry
    ) {
        // Given: page visible (from MakeAClaim), PNE hidden
        var caseDetails = buildCrossBorderCaseWithFlags(
            SOME_POSTCODE,
            selectedCountry,
            YesOrNo.YES,
            YesOrNo.NO
        );

        var result = EligibilityResult.builder()
            .status(EligibilityStatus.NOT_ELIGIBLE)
            .build();

        when(eligibilityService.checkEligibility(SOME_POSTCODE, selectedCountry))
            .thenReturn(result);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> resp =
            getMidEventForPage(event, "crossBorderPostcodeSelection")
                .handle(caseDetails, null);

        // Then: show PNE, keep cross-border for 'Previous'
        var data = resp.getData();
        assertThat(data.getShowPropertyNotEligiblePage()).isEqualTo(YesOrNo.YES);
        assertThat(data.getShowCrossBorderPage()).isEqualTo(YesOrNo.YES);
    }

    private static Stream<Arguments> eligibleCountries() {
        return Stream.of(
            arguments(LegislativeCountry.ENGLAND),
            arguments(LegislativeCountry.WALES)
        );
    }

    private CaseDetails<PCSCase, State> buildCrossBorderCaseWithFlags(
        String postcode,
        LegislativeCountry selectedCountry,
        YesOrNo showCrossBorderPage,
        YesOrNo showPropertyNotEligiblePage
    ) {
        var selected = DynamicStringListElement.builder()
            .code(selectedCountry.name())
            .label(selectedCountry.getLabel())
            .build();

        var dsl = DynamicStringList.builder()
            .value(selected)
            .build();

        var data = PCSCase.builder()
            .propertyAddress(AddressUK.builder().postCode(postcode).build())
            .crossBorderCountriesList(dsl)
            .showCrossBorderPage(showCrossBorderPage)
            .showPropertyNotEligiblePage(showPropertyNotEligiblePage)
            .build();

        var cd = new CaseDetails<PCSCase, State>();
        cd.setData(data);
        return cd;
    }
}
