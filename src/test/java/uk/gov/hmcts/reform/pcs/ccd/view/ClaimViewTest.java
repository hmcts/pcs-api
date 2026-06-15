package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.WalesDocuments;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimViewTest {

    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock(strictness = LENIENT)
    private ClaimEntity claimEntity;

    private ClaimView underTest;

    @BeforeEach
    void setUp() {
        pcsCase = PCSCase.builder().build();

        underTest = new ClaimView();
    }

    @Test
    void shouldMapBasicClaimFields() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(claimEntity.getAgainstTrespassers()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getDueToRentArrears()).thenReturn(YesOrNo.NO);
        when(claimEntity.getPreActionProtocolFollowed()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getMediationAttempted()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getSettlementAttempted()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getAdditionalDefendants()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getUnderlesseeOrMortgagee()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getAdditionalUnderlesseesOrMortgagees()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getGenAppExpected()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getLanguageUsed()).thenReturn(LanguageUsed.ENGLISH);
        when(claimEntity.getAdditionalDocsProvided()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getPreActionProtocolIncompleteExplanation()).thenReturn("explanation");
        when(claimEntity.getIsExemptLandlord()).thenReturn(VerticalYesNo.NO);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getClaimAgainstTrespassers()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getClaimDueToRentArrears()).isEqualTo(YesOrNo.NO);
        assertThat(pcsCase.getPreActionProtocolCompleted()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getMediationAttempted()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getSettlementAttempted()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getAddAnotherDefendant()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getHasUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getAddAdditionalUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.NO);
        assertThat(pcsCase.getApplicationWithClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getLanguageUsed()).isEqualTo(LanguageUsed.ENGLISH);
        assertThat(pcsCase.getWantToUploadDocuments()).isEqualTo(VerticalYesNo.YES);
        assertThat(pcsCase.getPreActionProtocolIncompleteExplanation()).isEqualTo("explanation");
        assertThat(pcsCase.getIsExemptLandlord()).isEqualTo(VerticalYesNo.NO);
    }

    @ParameterizedTest
    @MethodSource("complexClaimFieldsScenarios")
    void shouldMapComplexClaimFields(
        VerticalYesNo claimantSelect,
        String claimantDetails,
        VerticalYesNo defendantSelect,
        String defendantDetails,
        VerticalYesNo additionalReasonsProvided,
        String additionalReasons,
        ClaimantType claimantType
    ) {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(claimEntity.getClaimantCircumstancesProvided()).thenReturn(claimantSelect);
        when(claimEntity.getClaimantCircumstances()).thenReturn(claimantDetails);

        when(claimEntity.getDefendantCircumstancesProvided()).thenReturn(defendantSelect);
        when(claimEntity.getDefendantCircumstances()).thenReturn(defendantDetails);

        when(claimEntity.getAdditionalReasonsProvided()).thenReturn(additionalReasonsProvided);
        when(claimEntity.getAdditionalReasons()).thenReturn(additionalReasons);

        when(claimEntity.getClaimantType()).thenReturn(claimantType);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertThat(pcsCase.getClaimantCircumstances().getClaimantCircumstancesSelect()).isEqualTo(claimantSelect);
        assertThat(pcsCase.getClaimantCircumstances().getClaimantCircumstancesDetails()).isEqualTo(claimantDetails);

        assertThat(pcsCase.getDefendantCircumstances().getHasDefendantCircumstancesInfo()).isEqualTo(defendantSelect);
        assertThat(pcsCase.getDefendantCircumstances().getDefendantCircumstancesInfo()).isEqualTo(defendantDetails);

        assertThat(pcsCase.getAdditionalReasonsForPossession().getHasReasons()).isEqualTo(additionalReasonsProvided);
        assertThat(pcsCase.getAdditionalReasonsForPossession().getReasons()).isEqualTo(additionalReasons);

        if (claimantType != null) {
            assertThat(pcsCase.getClaimantType().getValue().getCode()).isEqualTo(claimantType.name());
            assertThat(pcsCase.getClaimantType().getValue().getLabel()).isEqualTo(claimantType.getLabel());
        } else {
            assertThat(pcsCase.getClaimantType()).isNull();
        }
    }

    @Test
    void shouldMapWalesRequiredDocumentAnswersWhenNoDocumentsExist() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(pcsCaseEntity.getDocuments()).thenReturn(List.of());
        when(claimEntity.getEnergyPerformanceCertificateProvided()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getGasSafetyReportProvided()).thenReturn(VerticalYesNo.YES);
        when(claimEntity.getElectricalInstallationConditionProvided()).thenReturn(VerticalYesNo.NO);
        when(claimEntity.getNoEnergyPerformanceCertificateReason()).thenReturn("No EPC reason");
        when(claimEntity.getNoGasSafetyReportReason()).thenReturn("No gas safety reason");
        when(claimEntity.getNoElectricalInstallationConditionReason()).thenReturn("No EICR reason");

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        WalesDocuments requiredDocumentsWales = pcsCase.getRequiredDocumentsWales();
        assertThat(requiredDocumentsWales.getHasEnergyPerformanceCertificate()).isEqualTo(VerticalYesNo.NO);
        assertThat(requiredDocumentsWales.getHasGasSafetyReport()).isEqualTo(VerticalYesNo.YES);
        assertThat(requiredDocumentsWales.getHasElectricalInstallationConditionReport()).isEqualTo(VerticalYesNo.NO);
        assertThat(requiredDocumentsWales.getNoEpcReason()).isEqualTo("No EPC reason");
        assertThat(requiredDocumentsWales.getNoGasReportReason()).isEqualTo("No gas safety reason");
        assertThat(requiredDocumentsWales.getNoEicrReason()).isEqualTo(
            "No EICR reason"
        );
        assertThat(requiredDocumentsWales.getEnergyPerformance()).isEmpty();
        assertThat(requiredDocumentsWales.getGasSafetyReport()).isEmpty();
        assertThat(requiredDocumentsWales.getElectricalInstallation()).isEmpty();
    }

    @Test
    void shouldMapOnlyMatchingWalesRequiredDocumentTypes() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(pcsCaseEntity.getDocuments()).thenReturn(List.of(
            documentEntity(DocumentType.ENERGY_PERFORMANCE_CERTIFICATE, "epc.pdf"),
            documentEntity(DocumentType.GAS_SAFETY_REPORT, "gas.pdf"),
            documentEntity(DocumentType.ELECTRICAL_INSTALLATION_CONDITION, "eicr.pdf"),
            documentEntity(DocumentType.OTHER, "other.pdf")
        ));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        WalesDocuments requiredDocumentsWales = pcsCase.getRequiredDocumentsWales();
        assertSingleDocument(requiredDocumentsWales.getEnergyPerformance(), "epc.pdf");
        assertSingleDocument(requiredDocumentsWales.getGasSafetyReport(), "gas.pdf");
        assertSingleDocument(requiredDocumentsWales.getElectricalInstallation(), "eicr.pdf");
    }

    @Test
    void shouldSetEmptyWalesRequiredDocumentListsWhenDocumentsExistButDoNotMatchRequiredTypes() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(claimEntity));
        when(pcsCaseEntity.getDocuments()).thenReturn(List.of(
            documentEntity(DocumentType.OTHER, "other.pdf"),
            documentEntity(DocumentType.RENT_STATEMENT, "rent-statement.pdf")
        ));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        WalesDocuments requiredDocumentsWales = pcsCase.getRequiredDocumentsWales();
        assertThat(requiredDocumentsWales.getEnergyPerformance()).isEmpty();
        assertThat(requiredDocumentsWales.getGasSafetyReport()).isEmpty();
        assertThat(requiredDocumentsWales.getElectricalInstallation()).isEmpty();
    }

    @Test
    void shouldNotPopulateAnyClaimFieldsWhenNoClaimsExist() {
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertThat(pcsCase.getClaimAgainstTrespassers()).isNull();
        assertThat(pcsCase.getClaimantCircumstances()).isNull();
        assertThat(pcsCase.getDefendantCircumstances()).isNull();
        assertThat(pcsCase.getAdditionalReasonsForPossession()).isNull();
        assertThat(pcsCase.getClaimantType()).isNull();
    }

    private static Stream<Arguments> complexClaimFieldsScenarios() {
        return Stream.of(
            Arguments.of(
                VerticalYesNo.YES, "claimant info",
                VerticalYesNo.NO, "defendant info",
                VerticalYesNo.YES, "some reasons",
                ClaimantType.PRIVATE_LANDLORD
            ),
            Arguments.of(
                null, null,
                VerticalYesNo.NO, "defendant info",
                VerticalYesNo.YES, "some reasons",
                ClaimantType.COMMUNITY_LANDLORD
            ),
            Arguments.of(
                VerticalYesNo.YES, "claimant info",
                null, null,
                VerticalYesNo.NO, null,
                ClaimantType.MORTGAGE_LENDER
            ),
            Arguments.of(
                VerticalYesNo.NO, "some claimant details",
                VerticalYesNo.YES, "some defendant details",
                VerticalYesNo.NO, "reasons",
                null
            )
        );
    }

    private static DocumentEntity documentEntity(DocumentType documentType, String fileName) {
        return DocumentEntity.builder()
            .id(documentId(fileName))
            .type(documentType)
            .url("http://dm-store/documents/" + fileName)
            .binaryUrl("http://dm-store/documents/" + fileName + "/binary")
            .fileName(fileName)
            .categoryId("category-" + fileName)
            .build();
    }

    private static UUID documentId(String fileName) {
        return UUID.nameUUIDFromBytes(fileName.getBytes());
    }

    private static void assertSingleDocument(List<ListValue<Document>> documents, String fileName) {
        assertThat(documents)
            .singleElement()
            .satisfies(listValue -> {
                // Case File View resolves documents by ListValue id - it must be set (HDPI-7064)
                assertThat(listValue.getId()).isEqualTo(documentId(fileName).toString());
                Document document = listValue.getValue();
                assertThat(document.getFilename()).isEqualTo(fileName);
                assertThat(document.getUrl()).isEqualTo("http://dm-store/documents/" + fileName);
                assertThat(document.getBinaryUrl()).isEqualTo("http://dm-store/documents/" + fileName + "/binary");
                assertThat(document.getCategoryId()).isEqualTo("category-" + fileName);
            });
    }

}
