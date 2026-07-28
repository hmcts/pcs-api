package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UploadAdditionalDocumentsDetailsTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        TextAreaValidationService textAreaValidationService = new TextAreaValidationService();
        setPageUnderTest(new UploadAdditionalDocumentsDetails(textAreaValidationService));
    }

    @Test
    void shouldNotReturnErrorsWhenDescriptionIsCorrectLength() {
        // Given
        AdditionalDocument doc = AdditionalDocument.builder()
                .description("Valid description")
                .build();

        PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder().value(doc).build()))
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride()).isNull();
        assertThat(response.getData()).isEqualTo(caseData);
    }

    @Test
    void shouldReturnValidationErrorsWhenDescriptionTooLong() {
        // Given
        String longDescription = "a".repeat(61);
        AdditionalDocument doc = AdditionalDocument.builder()
                .description(longDescription)
                .build();

        PCSCase caseData = PCSCase.builder()
                .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder().value(doc).build()))
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrorMessageOverride())
            .isNotNull()
            .contains("more than the maximum number of characters");
    }

    @Test
    void shouldCopyEnglandDocumentTypesUsingSharedDynamicListValues() {
        AdditionalDocument firstDocument = AdditionalDocument.builder()
            .documentTypeEngland(AdditionalDocumentTypeEngland.WITNESS_STATEMENT)
            .build();
        AdditionalDocument secondDocument = AdditionalDocument.builder()
            .documentTypeEngland(AdditionalDocumentTypeEngland.OTHER)
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .additionalDocuments(List.of(
                ListValue.<AdditionalDocument>builder().value(firstDocument).build(),
                ListValue.<AdditionalDocument>builder().value(secondDocument).build()
            ))
            .build();

        callMidEventHandler(caseData);

        DynamicList firstDocumentType = firstDocument.getDocumentType();
        DynamicList secondDocumentType = secondDocument.getDocumentType();

        assertThat(firstDocumentType).isNotNull();
        assertThat(secondDocumentType).isNotNull();
        assertThat(firstDocumentType.getListItems()).hasSize(AdditionalDocumentTypeEngland.values().length);
        assertThat(secondDocumentType.getListItems()).isSameAs(firstDocumentType.getListItems());
        assertThat(firstDocumentType.getValue().getLabel()).isEqualTo(AdditionalDocumentTypeEngland.WITNESS_STATEMENT.getLabel());
        assertThat(secondDocumentType.getValue().getLabel()).isEqualTo(AdditionalDocumentTypeEngland.OTHER.getLabel());
    }

    @Test
    void shouldCopyWalesDocumentTypesUsingSharedDynamicListValues() {
        AdditionalDocument firstDocument = AdditionalDocument.builder()
            .documentTypeWales(AdditionalDocumentTypeWales.WITNESS_STATEMENT)
            .build();
        AdditionalDocument secondDocument = AdditionalDocument.builder()
            .documentTypeWales(AdditionalDocumentTypeWales.OTHER)
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .additionalDocuments(List.of(
                ListValue.<AdditionalDocument>builder().value(firstDocument).build(),
                ListValue.<AdditionalDocument>builder().value(secondDocument).build()
            ))
            .build();

        callMidEventHandler(caseData);

        DynamicList firstDocumentType = firstDocument.getDocumentType();
        DynamicList secondDocumentType = secondDocument.getDocumentType();

        assertThat(firstDocumentType).isNotNull();
        assertThat(secondDocumentType).isNotNull();
        assertThat(firstDocumentType.getListItems()).hasSize(AdditionalDocumentTypeWales.values().length);
        assertThat(secondDocumentType.getListItems()).isSameAs(firstDocumentType.getListItems());
        assertThat(firstDocumentType.getValue().getLabel()).isEqualTo(AdditionalDocumentTypeWales.WITNESS_STATEMENT.getLabel());
        assertThat(secondDocumentType.getValue().getLabel()).isEqualTo(AdditionalDocumentTypeWales.OTHER.getLabel());
    }

    @Test
    void shouldReuseFirstAdditionalDocumentDocumentTypeListWhenPresent() {
        DynamicListElement witnessStatement = new DynamicListElement(UUID.randomUUID(),
            AdditionalDocumentTypeEngland.WITNESS_STATEMENT.getLabel());
        DynamicListElement other = new DynamicListElement(UUID.randomUUID(),
            AdditionalDocumentTypeEngland.OTHER.getLabel());
        List<DynamicListElement> existingDocumentTypes = List.of(witnessStatement, other);

        AdditionalDocument firstDocument = AdditionalDocument.builder()
            .documentType(new DynamicList(witnessStatement, existingDocumentTypes))
            .documentTypeEngland(AdditionalDocumentTypeEngland.WITNESS_STATEMENT)
            .build();
        AdditionalDocument secondDocument = AdditionalDocument.builder()
            .documentTypeEngland(AdditionalDocumentTypeEngland.OTHER)
            .build();

        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .additionalDocuments(List.of(
                ListValue.<AdditionalDocument>builder().value(firstDocument).build(),
                ListValue.<AdditionalDocument>builder().value(secondDocument).build()
            ))
            .build();

        callMidEventHandler(caseData);

        assertThat(secondDocument.getDocumentType()).isNotNull();
        assertThat(secondDocument.getDocumentType().getListItems()).isSameAs(existingDocumentTypes);
        assertThat(secondDocument.getDocumentType().getValue()).isSameAs(other);
    }

}
