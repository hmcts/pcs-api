package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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

    @BeforeEach
    void setUp() {
        setPageUnderTest(new CrossBorderPostcodeSelection(eligibilityService));
    }

    @ParameterizedTest
    @EnumSource(value = EligibilityStatus.class, names = {"ELIGIBLE", "NOT_ELIGIBLE"})
    void shouldSetLegislativeCountryWhenEligibilityMatchFound(EligibilityStatus eligibilityStatus) {
        // Given
        String postCode = "CF10 1EP";
        LegislativeCountry expectedLegislativeCountry = LegislativeCountry.WALES;

        AddressUK propertyAddress = AddressUK.builder()
            .postCode(postCode)
            .build();

        PCSCase caseData = PCSCase.builder()
            .propertyAddress(propertyAddress)
            .crossBorderCountriesList(createCountryListWithSelectedValue(expectedLegislativeCountry))
            .build();

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(eligibilityStatus)
            .legislativeCountry(expectedLegislativeCountry)
            .build();

        when(eligibilityService.checkEligibility(postCode, expectedLegislativeCountry)).thenReturn(eligibilityResult);

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getLegislativeCountry()).isEqualTo(expectedLegislativeCountry);
    }

    @ParameterizedTest
    @MethodSource("eligibilityScenarios")
    void shouldLogEligibilityBasedOnCountrySelection(
        String postcode,
        LegislativeCountry selectedCountry,
        EligibilityStatus status) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder().postCode(postcode).build())
            .crossBorderCountriesList(createCountryListWithSelectedValue(selectedCountry))
            .build();

        EligibilityResult eligibilityResult = EligibilityResult.builder()
            .status(status)
            .build();

        when(eligibilityService.checkEligibility(postcode, selectedCountry))
            .thenReturn(eligibilityResult);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response).isNotNull();
        PCSCase resultData = response.getData();
        assertThat(resultData).isNotNull();

        if (status == EligibilityStatus.NO_MATCH_FOUND) {
            assertThat(resultData.getShowPostcodeNotAssignedToCourt()).isEqualTo(YesOrNo.YES);
            assertThat(resultData.getLegislativeCountry()).isEqualTo(selectedCountry);

            switch (selectedCountry) {
                case ENGLAND -> assertThat(resultData.getPostcodeNotAssignedView()).isEqualTo("ENGLAND");
                case WALES -> assertThat(resultData.getPostcodeNotAssignedView()).isEqualTo("WALES");
                default -> assertThat(resultData.getPostcodeNotAssignedView()).isEqualTo("ALL_COUNTRIES");
            }
        } else if (status == EligibilityStatus.ELIGIBLE) {
            assertThat(resultData.getShowPostcodeNotAssignedToCourt()).isEqualTo(YesOrNo.NO);
        }
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

    private static DynamicStringList createCountryListWithSelectedValue(LegislativeCountry selectedCountry) {
        return DynamicStringList.builder()
            .value(DynamicStringListElement
                       .builder()
                       .code(selectedCountry.name())
                       .label(selectedCountry.getLabel())
                       .build())
            .build();
    }

    @ParameterizedTest
    @MethodSource("eligibleCountries")
    @DisplayName("ELIGIBLE keeps normal flow and cross-border page visible")
    void shouldContinueToClaimantInfoPageWhenCrossBorderPropertyIsEligible(
        LegislativeCountry selectedCountry) {

        // Given: page visible (from MakeAClaim), PNE hidden
        var caseData = buildCrossBorderCaseWithFlags(
            selectedCountry
        );

        var result = EligibilityResult.builder()
            .status(EligibilityStatus.ELIGIBLE)
            .build();

        when(eligibilityService.checkEligibility(SOME_POSTCODE, selectedCountry))
            .thenReturn(result);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> resp = callMidEventHandler(caseData);

        // Then
        var data = resp.getData();
        assertThat(data.getShowPropertyNotEligiblePage()).isEqualTo(YesOrNo.NO);
        assertThat(data.getShowCrossBorderPage()).isEqualTo(YesOrNo.YES);
    }

    @ParameterizedTest
    @MethodSource("eligibleCountries")
    @DisplayName("NOT_ELIGIBLE shows PropertyNotEligible and keeps cross-border page visible")
    void shouldShowPropertyNotEligiblePageWhenCrossBorderPropertyIsNotEligible(
        LegislativeCountry selectedCountry) {

        // Given: page visible (from MakeAClaim), PNE hidden
        var caseData = buildCrossBorderCaseWithFlags(
            selectedCountry
        );

        var result = EligibilityResult.builder()
            .status(EligibilityStatus.NOT_ELIGIBLE)
            .build();

        when(eligibilityService.checkEligibility(SOME_POSTCODE, selectedCountry))
            .thenReturn(result);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> resp = callMidEventHandler(caseData);

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

    private PCSCase buildCrossBorderCaseWithFlags(LegislativeCountry selectedCountry) {

        var selected = DynamicStringListElement.builder()
            .code(selectedCountry.name())
            .label(selectedCountry.getLabel())
            .build();

        var dynamicStringList = DynamicStringList.builder()
            .value(selected)
            .build();

        return PCSCase.builder()
            .propertyAddress(AddressUK.builder().postCode(CrossBorderPostcodeSelectionTest.SOME_POSTCODE).build())
            .crossBorderCountriesList(dynamicStringList)
            .showCrossBorderPage(YesOrNo.YES)
            .showPropertyNotEligiblePage(YesOrNo.NO)
            .build();
    }

}
