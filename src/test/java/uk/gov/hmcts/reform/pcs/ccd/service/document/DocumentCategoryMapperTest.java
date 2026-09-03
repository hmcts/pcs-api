package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.ClaimantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.DefendantDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.ClaimantDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload.wales.DefendantDocumentTypeWales;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentCategoryMapperTest {

    private DocumentCategoryMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentCategoryMapper();
    }

    @ParameterizedTest
    @MethodSource("claimantDocumentTypeScenarios")
    void shouldMapClaimantDocumentTypeToCategory(ClaimantDocumentType claimantDocumentType,
                                                 CaseFileCategory expectedCategory) {

        // When
        Optional<CaseFileCategory> actualCaseFileCategory = underTest.mapToCategory(claimantDocumentType);

        // Then
        if (expectedCategory != null) {
            assertThat(actualCaseFileCategory).contains(expectedCategory);
        } else {
            assertThat(actualCaseFileCategory).isEmpty();
        }
    }

    private static Stream<Arguments> claimantDocumentTypeScenarios() {
        return Stream.of(
            Arguments.arguments(null, null),
            Arguments.arguments(ClaimantDocumentType.WITNESS_STATEMENT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentType.RENT_STATEMENT, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(ClaimantDocumentType.TENANCY_AGREEMENT, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(ClaimantDocumentType.CERTIFICATE_OF_SERVICE, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentType.CORRESPONDENCE_FROM_DEFENDANT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentType.CORRESPONDENCE_FROM_CLAIMANT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentType.POSSESSION_NOTICE, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(ClaimantDocumentType.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION,
                                CaseFileCategory.STATEMENTS_OF_CASE),
            Arguments.arguments(ClaimantDocumentType.PHOTOGRAPHIC_EVIDENCE, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentType.INSPECTION_OR_REPORT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF, CaseFileCategory.CORRESPONDENCE),
            Arguments.arguments(ClaimantDocumentType.LEGAL_AID_CERTIFICATE, CaseFileCategory.CORRESPONDENCE),
            Arguments.arguments(ClaimantDocumentType.OTHER, CaseFileCategory.UNCATEGORISED_DOCUMENTS)
        );
    }

    @ParameterizedTest
    @MethodSource("claimantDocumentTypeWalesScenarios")
    void shouldMapClaimantDocumentTypeWalesToCategory(ClaimantDocumentTypeWales claimantDocumentTypeWales,
                                                      CaseFileCategory expectedCategory) {

        // When
        Optional<CaseFileCategory> actualCaseFileCategory = underTest.mapToCategory(claimantDocumentTypeWales);

        // Then
        if (expectedCategory != null) {
            assertThat(actualCaseFileCategory).contains(expectedCategory);
        } else {
            assertThat(actualCaseFileCategory).isEmpty();
        }
    }

    private static Stream<Arguments> claimantDocumentTypeWalesScenarios() {
        return Stream.of(
            Arguments.arguments(null, null),
            Arguments.arguments(ClaimantDocumentTypeWales.WITNESS_STATEMENT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentTypeWales.RENT_STATEMENT, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(ClaimantDocumentTypeWales.OCCUPATION_LICENCE, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(ClaimantDocumentTypeWales.CERTIFICATE_OF_SERVICE, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentTypeWales.CORRESPONDENCE_FROM_DEFENDANT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentTypeWales.CORRESPONDENCE_FROM_CLAIMANT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentTypeWales.POSSESSION_NOTICE, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(ClaimantDocumentTypeWales.NOTICE_FOR_SERVICE_OUT_OF_JURISDICTION,
                                CaseFileCategory.STATEMENTS_OF_CASE),
            Arguments.arguments(ClaimantDocumentTypeWales.PHOTOGRAPHIC_EVIDENCE, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentTypeWales.INSPECTION_OR_REPORT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(ClaimantDocumentTypeWales.CERTIFICATE_OF_SUITABILITY_AS_LF,
                                CaseFileCategory.CORRESPONDENCE),
            Arguments.arguments(ClaimantDocumentTypeWales.LEGAL_AID_CERTIFICATE, CaseFileCategory.CORRESPONDENCE),
            Arguments.arguments(ClaimantDocumentTypeWales.OTHER, CaseFileCategory.UNCATEGORISED_DOCUMENTS)
        );
    }

    @ParameterizedTest
    @MethodSource("defendantDocumentTypeScenarios")
    void shouldMapDefendantDocumentTypeToCategory(DefendantDocumentType defendantDocumentType,
                                                  CaseFileCategory expectedCategory) {

        // When
        Optional<CaseFileCategory> actualCaseFileCategory = underTest.mapToCategory(defendantDocumentType);

        // Then
        if (expectedCategory != null) {
            assertThat(actualCaseFileCategory).contains(expectedCategory);
        } else {
            assertThat(actualCaseFileCategory).isEmpty();
        }
    }

    private static Stream<Arguments> defendantDocumentTypeScenarios() {
        return Stream.of(
            Arguments.arguments(null, null),
            Arguments.arguments(DefendantDocumentType.RENT_STATEMENT, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(DefendantDocumentType.TENANCY_AGREEMENT, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(DefendantDocumentType.CORRESPONDENCE_FROM_DEFENDANT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(DefendantDocumentType.CORRESPONDENCE_FROM_CLAIMANT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(DefendantDocumentType.PHOTOGRAPHIC_EVIDENCE, CaseFileCategory.EVIDENCE),
            Arguments.arguments(DefendantDocumentType.CERTIFICATE_OF_SUITABILITY_AS_LF,
                                CaseFileCategory.CORRESPONDENCE),
            Arguments.arguments(DefendantDocumentType.LEGAL_AID_CERTIFICATE, CaseFileCategory.CORRESPONDENCE),
            Arguments.arguments(DefendantDocumentType.OTHER, CaseFileCategory.UNCATEGORISED_DOCUMENTS),
            Arguments.arguments(DefendantDocumentType.WITNESS_STATEMENT, CaseFileCategory.EVIDENCE)
        );
    }

    @ParameterizedTest
    @MethodSource("defendantDocumentTypeWalesScenarios")
    void shouldMapDefendantDocumentTypeWalesToCategory(DefendantDocumentTypeWales defendantDocumentTypeWales,
                                                       CaseFileCategory expectedCategory) {

        // When
        Optional<CaseFileCategory> actualCaseFileCategory = underTest.mapToCategory(defendantDocumentTypeWales);

        // Then
        if (expectedCategory != null) {
            assertThat(actualCaseFileCategory).contains(expectedCategory);
        } else {
            assertThat(actualCaseFileCategory).isEmpty();
        }
    }

    private static Stream<Arguments> defendantDocumentTypeWalesScenarios() {
        return Stream.of(
            Arguments.arguments(null, null),
            Arguments.arguments(DefendantDocumentTypeWales.RENT_STATEMENT, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(DefendantDocumentTypeWales.OCCUPATION_LICENCE, CaseFileCategory.PROPERTY_DOCUMENTS),
            Arguments.arguments(DefendantDocumentTypeWales.CORRESPONDENCE_FROM_DEFENDANT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(DefendantDocumentTypeWales.CORRESPONDENCE_FROM_CLAIMANT, CaseFileCategory.EVIDENCE),
            Arguments.arguments(DefendantDocumentTypeWales.PHOTOGRAPHIC_EVIDENCE, CaseFileCategory.EVIDENCE),
            Arguments.arguments(DefendantDocumentTypeWales.CERTIFICATE_OF_SUITABILITY_AS_LF,
                                CaseFileCategory.CORRESPONDENCE),
            Arguments.arguments(DefendantDocumentTypeWales.LEGAL_AID_CERTIFICATE, CaseFileCategory.CORRESPONDENCE),
            Arguments.arguments(DefendantDocumentTypeWales.OTHER, CaseFileCategory.UNCATEGORISED_DOCUMENTS),
            Arguments.arguments(DefendantDocumentTypeWales.WITNESS_STATEMENT, CaseFileCategory.EVIDENCE)
        );
    }

}
