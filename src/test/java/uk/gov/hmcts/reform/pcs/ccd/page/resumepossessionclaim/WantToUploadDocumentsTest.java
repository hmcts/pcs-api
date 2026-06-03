package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WantToUploadDocumentsTest extends BasePageTest {

    @Mock
    DraftCaseDataService draftCaseDataService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new WantToUploadDocuments(draftCaseDataService));
    }

    @ParameterizedTest(name = "England: {0} should {1}")
    @MethodSource("englandDocumentScenarios")
    void shouldFilterDocumentsForEngland(AdditionalDocumentType docType, String expectation, boolean shouldInclude) {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.ENGLAND)
                .wantToUploadDocuments(VerticalYesNo.YES)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        DynamicList documentTypeList =
                response.getData().getAdditionalDocuments().getFirst().getValue().getDocumentType();
        List<String> labels = extractLabels(documentTypeList);

        if (shouldInclude) {
            assertThat(labels).contains(docType.getLabel());
        } else {
            assertThat(labels).doesNotContain(docType.getLabel());
        }

        assertThat(documentTypeList.getListItems()).hasSize(13);
    }

    @ParameterizedTest(name = "Wales: {0} should {1}")
    @MethodSource("walesDocumentScenarios")
    void shouldFilterDocumentsForWales(AdditionalDocumentType docType, String expectation, boolean shouldInclude) {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .wantToUploadDocuments(VerticalYesNo.YES)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        DynamicList documentTypeList =
                response.getData().getAdditionalDocuments().getFirst().getValue().getDocumentType();
        List<String> labels = extractLabels(documentTypeList);

        if (shouldInclude) {
            assertThat(labels).contains(docType.getLabel());
        } else {
            assertThat(labels).doesNotContain(docType.getLabel());
        }

        assertThat(documentTypeList.getListItems()).hasSize(16);
    }

    @Test
    void shouldNotSetAdditionalDocumentsIfUserSelectsNo() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .wantToUploadDocuments(VerticalYesNo.NO)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getAdditionalDocuments()).isNull();
    }

    @Test
    void shouldSetDataFromDraftIfAvailable() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.ENGLAND)
                .wantToUploadDocuments(VerticalYesNo.YES)
                .build();

        long caseReference = 1234;
        UUID id = UUID.randomUUID();
        DynamicListElement value = DynamicListElement.builder()
                .code(id)
                .label(AdditionalDocumentType.WITNESS_STATEMENT.getLabel())
                .build();

        Optional<PCSCase> draftCaseData = Optional.of(PCSCase.builder()
                .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder()
                        .value(AdditionalDocument.builder()
                                .documentType(new DynamicList(value,
                                        List.of(new DynamicListElement(
                                                id, AdditionalDocumentType.WITNESS_STATEMENT.getLabel()))))
                                .description("Witness Statement")
                                .build())
                        .build()))
                .build());

        when(draftCaseDataService.getUnsubmittedCaseData(caseReference, EventId.resumePossessionClaim))
                .thenReturn(draftCaseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        AdditionalDocument addDoc = response.getData().getAdditionalDocuments().getFirst().getValue();
        assertThat(addDoc.getDescription()).isEqualTo("Witness Statement");
        assertThat(addDoc.getDocumentType().getListItems())
                .extracting(DynamicListElement::getLabel)
                .containsExactly(AdditionalDocumentType.WITNESS_STATEMENT.getLabel());
    }

    @Test
    void shouldSetAdditionalDocumentsToNullIfNotInDraftData() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.ENGLAND)
                .wantToUploadDocuments(VerticalYesNo.YES)
                .build();

        long caseReference = 1234;

        Optional<PCSCase> draftCaseData = Optional.of(PCSCase.builder().build());

        when(draftCaseDataService.getUnsubmittedCaseData(caseReference, EventId.resumePossessionClaim))
                .thenReturn(draftCaseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getAdditionalDocuments().getFirst().getValue().getDescription()).isNull();
        assertThat(response.getData().getAdditionalDocuments().getFirst().getValue().getDocumentType().getValue())
                .isNull();
    }

    private static Stream<Arguments> englandDocumentScenarios() {
        return Stream.of(
                // England-specific documents
                arguments(AdditionalDocumentType.TENANCY_AGREEMENT, "be included", true),

                // Wales-only documents should be excluded
                arguments(AdditionalDocumentType.OCCUPATION_LICENCE, "be excluded", false),
                arguments(AdditionalDocumentType.ENERGY_PERFORMANCE_CERTIFICATE, "be excluded", false),
                arguments(AdditionalDocumentType.GAS_SAFETY_CERTIFICATE, "be excluded", false),
                arguments(AdditionalDocumentType.EICR_REPORT, "be excluded", false),

                // Common documents
                arguments(AdditionalDocumentType.WITNESS_STATEMENT, "be included", true),
                arguments(AdditionalDocumentType.RENT_STATEMENT, "be included", true),
                arguments(AdditionalDocumentType.CERTIFICATE_OF_SERVICE, "be included", true),
                arguments(AdditionalDocumentType.CORRESPONDENCE_FROM_DEFENDANT, "be included", true),
                arguments(AdditionalDocumentType.CORRESPONDENCE_FROM_CLAIMANT, "be included", true),
                arguments(AdditionalDocumentType.POSSESSION_NOTICE, "be included", true),
                arguments(AdditionalDocumentType.LEGAL_AID_CERTIFICATE, "be included", true),
                arguments(AdditionalDocumentType.OTHER, "be included", true)
        );
    }

    private static Stream<Arguments> walesDocumentScenarios() {
        return Stream.of(
                // Wales-specific documents
                arguments(AdditionalDocumentType.OCCUPATION_LICENCE, "be included", true),
                arguments(AdditionalDocumentType.ENERGY_PERFORMANCE_CERTIFICATE, "be included", true),
                arguments(AdditionalDocumentType.GAS_SAFETY_CERTIFICATE, "be included", true),
                arguments(AdditionalDocumentType.EICR_REPORT, "be included", true),

                // England-only documents should be excluded
                arguments(AdditionalDocumentType.TENANCY_AGREEMENT, "be excluded", false),

                // Common documents
                arguments(AdditionalDocumentType.WITNESS_STATEMENT, "be included", true),
                arguments(AdditionalDocumentType.RENT_STATEMENT, "be included", true),
                arguments(AdditionalDocumentType.CERTIFICATE_OF_SERVICE, "be included", true),
                arguments(AdditionalDocumentType.CORRESPONDENCE_FROM_DEFENDANT, "be included", true),
                arguments(AdditionalDocumentType.CORRESPONDENCE_FROM_CLAIMANT, "be included", true),
                arguments(AdditionalDocumentType.POSSESSION_NOTICE, "be included", true),
                arguments(AdditionalDocumentType.LEGAL_AID_CERTIFICATE, "be included", true),
                arguments(AdditionalDocumentType.OTHER, "be included", true)
        );
    }

    // Helper method to extract labels from DynamicList
    private List<String> extractLabels(DynamicList documentTypeList) {
        return documentTypeList.getListItems().stream()
            .map(DynamicListElement::getLabel)
            .collect(Collectors.toList());
    }
}