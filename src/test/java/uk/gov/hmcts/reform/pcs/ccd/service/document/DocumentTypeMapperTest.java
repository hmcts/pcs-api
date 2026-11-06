package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.ClaimantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DefendantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.ClaimantDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.DefendantDocumentTypeWales;

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
        @EnumSource(AdditionalDocumentType.class)
        void shouldMapAdditionalDocumentTypeToDocumentType(AdditionalDocumentType additionalDocumentType) {
            // When
            DocumentType actualDocumentType = underTest.mapToDocumentType(additionalDocumentType);

            // Then
            assertThat(actualDocumentType).isEqualTo(DocumentType.valueOf(additionalDocumentType.name()));
        }

        @Test
        void shouldReturnNullWhenAdditionalDocumentTypeIsNull() {
            // When
            DocumentType actualDocumentType = underTest.mapToDocumentType((AdditionalDocumentType) null);

            // Then
            assertThat(actualDocumentType).isNull();
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
