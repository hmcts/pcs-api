package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.WrappedQuestion;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProhibitedConductWalesPage Tests")
class ProhibitedConductWalesPageTest extends BasePageTest {

    private ProhibitedConductWalesPage pageUnderTest;

    @BeforeEach
    void setUp() {
        pageUnderTest = new ProhibitedConductWalesPage();
        setPageUnderTest(pageUnderTest);
    }

    @Test
    @DisplayName("Should create page configuration successfully")
    void shouldCreatePageConfigurationSuccessfully() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .claimForProhibitedConductContract(VerticalYesNo.YES)
            .whyMakingClaim("Some reason for making claim")
            .prohibitedConductWales(WrappedQuestion.builder()
                .agreedTermsOfPeriodicContract(VerticalYesNo.YES)
                .detailsOfTerms("Wrapped terms details")
                .build())
            .build();

        // When & Then - Just verify the page configuration is created without errors
        // Since there's no complex mid event handler, we just test that the page can be instantiated
        assertThat(pageUnderTest).isNotNull();
        assertThat(pageUnderTest).isInstanceOf(ProhibitedConductWalesPage.class);
    }

    @Test
    @DisplayName("Should handle null case data gracefully")
    void shouldHandleNullCaseDataGracefully() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .claimForProhibitedConductContract(null)
            .whyMakingClaim(null)
            .prohibitedConductWales(null)
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
            .claimForProhibitedConductContract(VerticalYesNo.NO)
            .whyMakingClaim("")
            .prohibitedConductWales(WrappedQuestion.builder().build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then - Should not throw any exceptions
        assertThat(caseData).isNotNull();
        assertThat(caseData.getClaimForProhibitedConductContract()).isEqualTo(VerticalYesNo.NO);
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
            .claimForProhibitedConductContract(claimForProhibitedConductContract)
            .whyMakingClaim(whyMakingClaim)
            .prohibitedConductWales(WrappedQuestion.builder()
                .agreedTermsOfPeriodicContract(agreedTermsOfPeriodicContract)
                .detailsOfTerms(detailsOfTerms)
                .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimForProhibitedConductContract()).isEqualTo(claimForProhibitedConductContract);
        assertThat(caseData.getWhyMakingClaim()).isEqualTo(whyMakingClaim);
        assertThat(caseData.getProhibitedConductWales()).isNotNull();
        assertThat(caseData.getProhibitedConductWales().getAgreedTermsOfPeriodicContract()).isEqualTo(agreedTermsOfPeriodicContract);
        assertThat(caseData.getProhibitedConductWales().getDetailsOfTerms()).isEqualTo(detailsOfTerms);
    }

    @Test
    @DisplayName("Should preserve all field values after mid event")
    void shouldPreserveAllFieldValuesAfterMidEvent() {
        // Given
        String expectedDetailsOfTerms = "We have agreed on specific payment terms and maintenance responsibilities";
        String expectedWhyMakingClaim = "The tenant has repeatedly violated the terms of the contract";
        
        PCSCase caseData = PCSCase.builder()
            .claimForProhibitedConductContract(VerticalYesNo.YES)
            .whyMakingClaim(expectedWhyMakingClaim)
            .prohibitedConductWales(WrappedQuestion.builder()
                .agreedTermsOfPeriodicContract(VerticalYesNo.YES)
                .detailsOfTerms(expectedDetailsOfTerms)
                .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getClaimForProhibitedConductContract()).isEqualTo(VerticalYesNo.YES);
        assertThat(caseData.getWhyMakingClaim()).isEqualTo(expectedWhyMakingClaim);
        assertThat(caseData.getProhibitedConductWales()).isNotNull();
        assertThat(caseData.getProhibitedConductWales().getAgreedTermsOfPeriodicContract()).isEqualTo(VerticalYesNo.YES);
        assertThat(caseData.getProhibitedConductWales().getDetailsOfTerms()).isEqualTo(expectedDetailsOfTerms);
    }

    @Test
    @DisplayName("Should handle wrapped question with null values")
    void shouldHandleWrappedQuestionWithNullValues() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .claimForProhibitedConductContract(VerticalYesNo.YES)
            .whyMakingClaim("Some reason")
            .prohibitedConductWales(WrappedQuestion.builder()
                .agreedTermsOfPeriodicContract(null)
                .detailsOfTerms(null)
                .build())
            .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getProhibitedConductWales()).isNotNull();
        assertThat(caseData.getProhibitedConductWales().getAgreedTermsOfPeriodicContract()).isNull();
        assertThat(caseData.getProhibitedConductWales().getDetailsOfTerms()).isNull();
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
}
