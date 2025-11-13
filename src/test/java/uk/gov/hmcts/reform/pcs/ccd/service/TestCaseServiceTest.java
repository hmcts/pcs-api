package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestCaseService - Instancio 1.0.4 based PCSCase Population")
class TestCaseServiceTest {

    private TestCaseService underTest;

    @BeforeEach
    void setUp() {
        underTest = new TestCaseService();
    }

    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Should successfully generate a PCSCase instance")
        void shouldGeneratePCSCaseInstance() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should populate String fields")
        void shouldPopulateStringFields() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getFeeAmount()).isNotNull();
            assertThat(result.getClaimantName()).isNotNull();
            assertThat(result.getOrganisationName()).isNotNull();
        }

        @Test
        @DisplayName("Should populate Integer fields")
        void shouldPopulateIntegerFields() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getCaseManagementLocation()).isNotNull();
        }

        @Test
        @DisplayName("Should populate YesOrNo enum fields")
        void shouldPopulateYesOrNoFields() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getHasUnsubmittedCaseData()).isNotNull();
            assertThat(result.getResumeClaimKeepAnswers()).isNotNull();
            assertThat(result.getGroundsForPossession()).isNotNull();
        }

        @Test
        @DisplayName("Should populate VerticalYesNo enum fields")
        void shouldPopulateVerticalYesNoFields() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getIsClaimantNameCorrect()).isNotNull();
            assertThat(result.getPreActionProtocolCompleted()).isNotNull();
            assertThat(result.getMediationAttempted()).isNotNull();
        }

        @Test
        @DisplayName("Should populate LocalDate fields")
        void shouldPopulateLocalDateFields() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getNoticePostedDate()).isNotNull();
            assertThat(result.getTenancyLicenceDate()).isNotNull();
        }

        @Test
        @DisplayName("Should populate LocalDateTime fields")
        void shouldPopulateLocalDateTimeFields() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getNoticeHandedOverDateTime()).isNotNull();
            assertThat(result.getNoticeEmailSentDateTime()).isNotNull();
        }

        @Test
        @DisplayName("Should populate LegislativeCountry enum field")
        void shouldPopulateLegislativeCountryField() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getLegislativeCountry()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Complex Object Population Tests")
    class ComplexObjectTests {

        @Test
        @DisplayName("Should populate AddressUK with required fields")
        void shouldPopulateAddressUKObjects() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getPropertyAddress()).isNotNull();
            assertThat(result.getPropertyAddress().getAddressLine1()).isNotBlank();
            assertThat(result.getPropertyAddress().getPostTown()).isNotBlank();
            assertThat(result.getPropertyAddress().getPostCode()).isNotBlank();
        }

        @Test
        @DisplayName("Should populate DefendantDetails complex object")
        void shouldPopulateDefendantDetails() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getDefendant1()).isNotNull();
            assertThat(result.getDefendant1().getNameKnown()).isNotNull();
        }

        @Test
        @DisplayName("Should populate DefendantCircumstances complex object")
        void shouldPopulateDefendantCircumstances() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getDefendantCircumstances()).isNotNull();
            assertThat(result.getDefendantCircumstances().getHasDefendantCircumstancesInfo()).isNotNull();
        }

        @Test
        @DisplayName("Should populate ClaimantCircumstances complex object")
        void shouldPopulateClaimantCircumstances() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getClaimantCircumstances()).isNotNull();
            assertThat(result.getClaimantCircumstances().getClaimantCircumstancesSelect()).isNotNull();
        }

        @Test
        @DisplayName("Should populate WalesHousingAct complex object")
        void shouldPopulateWalesHousingAct() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getWalesHousingAct()).isNotNull();
        }

        @Test
        @DisplayName("Should populate RentArrearsGroundsReasons complex object")
        void shouldPopulateRentArrearsGroundsReasons() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getRentArrearsGroundsReasons()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Collection Population Tests")
    class CollectionTests {

        @Test
        @DisplayName("Should populate List collections")
        void shouldPopulateListCollections() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getDefendants()).isNotNull();
            assertThat(result.getNoticeDocuments()).isNotNull();
            assertThat(result.getTenancyLicenceDocuments()).isNotNull();
            assertThat(result.getEnforcementOrder().getAdditionalInformation().getAdditionalInformationDetails())
                .isNotNull();
        }

        @Test
        @DisplayName("Should populate Set collections")
        void shouldPopulateSetCollections() {
            // When
            PCSCase result = underTest.generateTestPCSCase();

            // Then
            assertThat(result.getRentArrearsGrounds()).isNotNull();
            assertThat(result.getCopyOfRentArrearsGrounds()).isNotNull();
        }
    }

}
