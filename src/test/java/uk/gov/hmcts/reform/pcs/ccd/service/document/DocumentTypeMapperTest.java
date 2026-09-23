package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.ClaimantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DefendantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.ClaimantDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.DefendantDocumentTypeWales;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class DocumentTypeMapperTest {

    private DocumentTypeMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentTypeMapper();
    }

    @Nested
    @DisplayName("Additional document type tests")
    class AdditionalDocumentTypeTests {

        @ParameterizedTest
        @MethodSource("additionalDocumentTypeScenarios")
        void shouldMapAdditionalDocumentTypeToDocumentType(AdditionalDocumentType additionalDocumentType,
                                                           DocumentType expectedDocumentType) {
            // When
            DocumentType actualDocumentType = underTest.mapToDocumentType(additionalDocumentType);

            // Then
            assertThat(actualDocumentType).isEqualTo(expectedDocumentType);
        }

        @Test
        void shouldReturnNullWhenAdditionalDocumentTypeIsNull() {
            // When
            DocumentType actualDocumentType = underTest.mapToDocumentType((AdditionalDocumentType) null);

            // Then
            assertThat(actualDocumentType).isNull();
        }

        private static Stream<Arguments> additionalDocumentTypeScenarios() {
            return Stream.of(
                Arguments.of(
                    null,
                    null
                ),
                Arguments.of(
                    AdditionalDocumentType.WITNESS_STATEMENT,
                    DocumentType.WITNESS_STATEMENT
                ),
                Arguments.of(
                    AdditionalDocumentType.RENT_STATEMENT,
                    DocumentType.RENT_STATEMENT
                ),
                Arguments.of(
                    AdditionalDocumentType.TENANCY_AGREEMENT,
                    DocumentType.TENANCY_AGREEMENT
                ),
                Arguments.of(
                    AdditionalDocumentType.CERTIFICATE_OF_SERVICE,
                    DocumentType.CERTIFICATE_OF_SERVICE
                ),
                Arguments.of(
                    AdditionalDocumentType.CORRESPONDENCE_FROM_DEFENDANT,
                    DocumentType.CORRESPONDENCE_FROM_DEFENDANT
                ),
                Arguments.of(
                    AdditionalDocumentType.CORRESPONDENCE_FROM_CLAIMANT,
                    DocumentType.CORRESPONDENCE_FROM_CLAIMANT
                ),
                Arguments.of(
                    AdditionalDocumentType.POSSESSION_NOTICE,
                    DocumentType.POSSESSION_NOTICE
                ),
                Arguments.of(
                    AdditionalDocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION,
                    DocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION
                ),
                Arguments.of(
                    AdditionalDocumentType.PHOTOGRAPHIC_EVIDENCE,
                    DocumentType.PHOTOGRAPHIC_EVIDENCE
                ),
                Arguments.of(
                    AdditionalDocumentType.INSPECTION_OR_REPORT,
                    DocumentType.INSPECTION_OR_REPORT
                ),
                Arguments.of(
                    AdditionalDocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF,
                    DocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF
                ),
                Arguments.of(
                    AdditionalDocumentType.LEGAL_AID_CERTIFICATE,
                    DocumentType.LEGAL_AID_CERTIFICATE
                ),
                Arguments.of(
                    AdditionalDocumentType.OTHER,
                    DocumentType.OTHER
                )
            );
        }
    }

    @Nested
    @DisplayName("Claimant document type tests")
    class ClaimantDocumentTypeTests {

        @ParameterizedTest
        @EnumSource(ClaimantDocumentType.class)
        void shouldMapClaimantDocumentTypeToDocumentType(ClaimantDocumentType claimantDocumentType) {
            assertThat(underTest.mapToDocumentType(claimantDocumentType))
                .isEqualTo(DocumentType.valueOf(claimantDocumentType.name()));
        }

        @Test
        void shouldReturnNullWhenClaimantDocumentTypeIsNull() {
            assertThat(underTest.mapToDocumentType((ClaimantDocumentType) null)).isNull();
        }

        @ParameterizedTest
        @EnumSource(ClaimantDocumentTypeWales.class)
        void shouldMapCClaimantDocumentTypeWalesToDocumentType(ClaimantDocumentTypeWales claimantDocumentTypeWales) {
            assertThat(underTest.mapToDocumentType(claimantDocumentTypeWales))
                .isEqualTo(DocumentType.valueOf(claimantDocumentTypeWales.name()));
        }

        @Test
        void shouldReturnNullWhenClaimantDocumentTypeWalesIsNull() {
            assertThat(underTest.mapToDocumentType((ClaimantDocumentTypeWales) null)).isNull();
        }

    }

    @Nested
    @DisplayName("Defendant document type tests")
    class DefendantDocumentTypeTests {

        @ParameterizedTest
        @EnumSource(DefendantDocumentType.class)
        void shouldMapDefendantDocumentTypeToDocumentType(DefendantDocumentType defendantDocumentType) {
            assertThat(underTest.mapToDocumentType(defendantDocumentType))
                .isEqualTo(DocumentType.valueOf(defendantDocumentType.name()));
        }

        @Test
        void shouldReturnNullWhenDefendantDocumentTypeIsNull() {
            assertThat(underTest.mapToDocumentType((DefendantDocumentType) null)).isNull();
        }

        @ParameterizedTest
        @EnumSource(DefendantDocumentTypeWales.class)
        void shouldMapCDefendantDocumentTypeWalesToDocumentType(DefendantDocumentTypeWales defendantDocumentTypeWales) {
            assertThat(underTest.mapToDocumentType(defendantDocumentTypeWales))
                .isEqualTo(DocumentType.valueOf(defendantDocumentTypeWales.name()));
        }

        @Test
        void shouldReturnNullWhenDefendantDocumentTypeWalesIsNull() {
            assertThat(underTest.mapToDocumentType((DefendantDocumentTypeWales) null)).isNull();
        }

    }

}
