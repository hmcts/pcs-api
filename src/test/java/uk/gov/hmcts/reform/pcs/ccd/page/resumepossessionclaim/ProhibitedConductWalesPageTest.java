package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ProhibitedConductWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProhibitedConductWalesPage Tests")
class ProhibitedConductWalesPageTest extends BasePageTest {

    private ProhibitedConductWalesPage pageUnderTest;

    @Mock
    private TextAreaValidationService textAreaValidationService;

    @BeforeEach
    void setUp() {
        lenient().when(textAreaValidationService.createValidationResponse(any(), any()))
            .thenAnswer(invocation -> {
                PCSCase caseData = invocation.getArgument(0);
                List<String> errors = invocation.getArgument(1);
                return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                    .data(caseData)
                    .errors(errors)
                    .build();
            });

        pageUnderTest = new ProhibitedConductWalesPage(textAreaValidationService);
        setPageUnderTest(pageUnderTest);
    }

    @Test
    @DisplayName("Should create page configuration successfully")
    void shouldCreatePageConfigurationSuccessfully() {
        assertThat(pageUnderTest).isNotNull();
        assertThat(pageUnderTest).isInstanceOf(ProhibitedConductWalesPage.class);
    }

    @Test
    @DisplayName("Should handle null case data gracefully")
    void shouldHandleNullCaseDataGracefully() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .prohibitedConductWales(
                ProhibitedConductWales.builder()
                .prohibitedConductWalesClaim(null)
                .prohibitedConductWalesWhyMakingClaim(null)
                    .build()
            )
            .build();

        // When
        callMidEventHandler(caseData);

        // Then - Should not throw any exceptions
        assertThat(caseData).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty case data gracefully")
    void shouldHandleEmptyCaseDataGracefully() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .prohibitedConductWales(
                ProhibitedConductWales.builder()
                .prohibitedConductWalesClaim(VerticalYesNo.NO)
                .prohibitedConductWalesWhyMakingClaim("")
                    .build()
            )
            .build();

        // When
        callMidEventHandler(caseData);

        // Then - Should not throw any exceptions
        assertThat(caseData).isNotNull();
        assertThat(caseData.getProhibitedConductWales().getProhibitedConductWalesClaim()).isEqualTo(VerticalYesNo.NO);
    }

    @ParameterizedTest
    @MethodSource("prohibitedConductContractScenarios")
    @DisplayName("Should handle different prohibited conduct contract scenarios: {0}")
    void shouldHandleDifferentProhibitedConductContractScenarios(
        @SuppressWarnings("unused") String testDescription,
        VerticalYesNo claimForProhibitedConductContract,
        VerticalYesNo agreedTermsOfPeriodicContract,
        String detailsOfTerms,
        String whyMakingClaim) {

        // Given
        PCSCase caseData = PCSCase.builder()
            .prohibitedConductWales(
                ProhibitedConductWales.builder()
                .prohibitedConductWalesClaim(claimForProhibitedConductContract)
                .prohibitedConductWalesWhyMakingClaim(whyMakingClaim)
                .agreedTermsOfPeriodicContractOption(agreedTermsOfPeriodicContract)
                .detailsOfTermsText(detailsOfTerms)
                    .build()
            )
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getProhibitedConductWales().getProhibitedConductWalesClaim())
            .isEqualTo(claimForProhibitedConductContract);
        assertThat(caseData.getProhibitedConductWales().getProhibitedConductWalesWhyMakingClaim())
            .isEqualTo(whyMakingClaim);
        assertThat(caseData.getProhibitedConductWales()).isNotNull();
        assertThat(caseData.getProhibitedConductWales()
            .getAgreedTermsOfPeriodicContractOption()).isEqualTo(agreedTermsOfPeriodicContract);
        assertThat(caseData.getProhibitedConductWales()
            .getDetailsOfTermsText()).isEqualTo(detailsOfTerms);
    }

    @Test
    @DisplayName("Should preserve all field values after mid event")
    void shouldPreserveAllFieldValuesAfterMidEvent() {
        // Given
        String expectedDetailsOfTerms = "We have agreed on specific payment terms and maintenance responsibilities";
        String expectedWhyMakingClaim = "The tenant has repeatedly violated the terms of the contract";

        PCSCase caseData = PCSCase.builder()
            .prohibitedConductWales(
                ProhibitedConductWales.builder()
                .prohibitedConductWalesClaim(VerticalYesNo.YES)
                .prohibitedConductWalesWhyMakingClaim(expectedWhyMakingClaim)
                .agreedTermsOfPeriodicContractOption(VerticalYesNo.YES)
                .detailsOfTermsText(expectedDetailsOfTerms)
                    .build()
            )
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getProhibitedConductWales().getProhibitedConductWalesClaim())
            .isEqualTo(VerticalYesNo.YES);
        assertThat(caseData.getProhibitedConductWales().getProhibitedConductWalesWhyMakingClaim())
            .isEqualTo(expectedWhyMakingClaim);
        assertThat(caseData.getProhibitedConductWales()).isNotNull();
        assertThat(caseData.getProhibitedConductWales()
            .getAgreedTermsOfPeriodicContractOption()).isEqualTo(VerticalYesNo.YES);
        assertThat(caseData.getProhibitedConductWales()
            .getDetailsOfTermsText()).isEqualTo(expectedDetailsOfTerms);
    }

    @Test
    @DisplayName("Should handle wrapped question with null values")
    void shouldHandleWrappedQuestionWithNullValues() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .prohibitedConductWales(
                ProhibitedConductWales.builder()
                .prohibitedConductWalesClaim(VerticalYesNo.YES)
                .prohibitedConductWalesWhyMakingClaim("Some reason")
                .agreedTermsOfPeriodicContractOption(null)
                .detailsOfTermsText(null)
                    .build()
            )
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getProhibitedConductWales()).isNotNull();
        assertThat(caseData.getProhibitedConductWales().getAgreedTermsOfPeriodicContractOption()).isNull();
        assertThat(caseData.getProhibitedConductWales().getDetailsOfTermsText()).isNull();
    }

    private static Stream<Arguments> prohibitedConductContractScenarios() {
        return Stream.of(
            Arguments.of(
                "User claims prohibited conduct contract and agrees terms",
                VerticalYesNo.YES,
                VerticalYesNo.YES,
                "We have agreed on specific maintenance terms",
                "The tenant has breached contract conditions"
            ),
            Arguments.of(
                "User claims prohibited conduct contract but disagrees on terms",
                VerticalYesNo.YES,
                VerticalYesNo.NO,
                null,
                "The tenant has violated multiple contract terms"
            ),
            Arguments.of(
                "User does not claim prohibited conduct contract",
                VerticalYesNo.NO,
                null,
                null,
                null
            ),
            Arguments.of(
                "User claims prohibited conduct contract with empty text fields",
                VerticalYesNo.YES,
                VerticalYesNo.YES,
                "",
                ""
            )
        );
    }

    @Test
    @DisplayName("Should validate both textarea fields when both conditions are met")
    void shouldValidateBothTextAreaFieldsWhenBothConditionsAreMet() {
        // Given
        String longText = "a".repeat(251); // Exceeds 250 character limit
        PCSCase caseData = PCSCase.builder()
            .prohibitedConductWales(
                ProhibitedConductWales.builder()
                .prohibitedConductWalesClaim(VerticalYesNo.YES)
                .prohibitedConductWalesWhyMakingClaim(longText)
                .agreedTermsOfPeriodicContractOption(VerticalYesNo.YES)
                .detailsOfTermsText(longText)
                    .build()
            )
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        doAnswer(invocation -> {
            List<String> errors = invocation.getArgument(3);
            errors.add("In ‘" + invocation.getArgument(1) + "’, you have entered more than the maximum number of "
                + invocation.getArgument(2) + " characters");
            return null;
        }).when(textAreaValidationService).validateTextArea(any(), any(), anyInt(), any());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = pageUnderTest.midEvent(caseDetails, caseDetails);

        // Then
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors()).hasSize(2);
        assertThat(response.getErrors()).contains(
            "In ‘Why are you making this claim?’, you have entered more than the maximum number of "
                + "250 characters"
        );
        assertThat(response.getErrors()).contains(
            "In ‘Give details of the terms you’ve agreed’, you have entered more than the maximum number of "
                + "250 characters"
        );
    }

    @Test
    @DisplayName("Should validate textarea fields and return no errors when within character limit")
    void shouldValidateTextAreaFieldsAndReturnNoErrorsWhenWithinCharacterLimit() {
        // Given
        String validText = "a".repeat(250); // Exactly 250 characters
        PCSCase caseData = PCSCase.builder()
            .prohibitedConductWales(
                ProhibitedConductWales.builder()
                .prohibitedConductWalesClaim(VerticalYesNo.YES)
                .prohibitedConductWalesWhyMakingClaim(validText)
                .agreedTermsOfPeriodicContractOption(VerticalYesNo.YES)
                .detailsOfTermsText(validText)
                    .build()
            )
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        lenient().when(textAreaValidationService.validateMultipleTextAreas(any()))
            .thenReturn(new ArrayList<>());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = pageUnderTest.midEvent(caseDetails, caseDetails);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    @DisplayName("Should only validate 'Why are you making this claim?' when user claims prohibited conduct "
        + "but disagrees on terms")
    void shouldOnlyValidateWhyMakingClaimWhenUserClaimsButDisagreesOnTerms() {
        // Given
        String longText = "a".repeat(251); // Exceeds 250 character limit
        PCSCase caseData = PCSCase.builder()
            .prohibitedConductWales(
                ProhibitedConductWales.builder()
                .prohibitedConductWalesClaim(VerticalYesNo.YES)
                .prohibitedConductWalesWhyMakingClaim(longText)
                .agreedTermsOfPeriodicContractOption(VerticalYesNo.NO)
                .detailsOfTermsText(longText)
                    .build()
            )
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        doAnswer(invocation -> {
            List<String> errors = invocation.getArgument(3);
            errors.add("In ‘" + invocation.getArgument(1) + "’, you have entered more than the maximum number of "
                + invocation.getArgument(2) + " characters");
            return null;
        }).when(textAreaValidationService).validateTextArea(any(), any(), anyInt(), any());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = pageUnderTest.midEvent(caseDetails, caseDetails);

        // Then
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors()).hasSize(1); // Only one error for "Why are you making this claim?"
        assertThat(response.getErrors()).contains(
            "In ‘Why are you making this claim?’, you have entered more than the maximum number of "
                + "250 characters"
        );
        assertThat(response.getErrors()).doesNotContain(
            "In ‘Give details of the terms you’ve agreed’, you have entered more than the maximum number of "
                + "250 characters"
        );
    }

    @Test
    @DisplayName("Should handle null wrapped question gracefully during validation")
    void shouldHandleNullWrappedQuestionGracefullyDuringValidation() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .prohibitedConductWales(
                ProhibitedConductWales.builder()
                .prohibitedConductWalesClaim(VerticalYesNo.YES)
                .prohibitedConductWalesWhyMakingClaim("Valid reason")
                    .build()
            )
            .build();

        CaseDetails<PCSCase, State> caseDetails = CaseDetails.<PCSCase, State>builder()
            .data(caseData)
            .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = pageUnderTest.midEvent(caseDetails, caseDetails);

        // Then
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData()).isEqualTo(caseData);
    }
}

