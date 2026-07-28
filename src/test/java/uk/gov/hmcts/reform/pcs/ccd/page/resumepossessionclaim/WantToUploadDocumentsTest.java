package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WantToUploadDocumentsTest extends BasePageTest {

    @Mock
    DraftCaseDataService draftCaseDataService;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new WantToUploadDocuments(draftCaseDataService));
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
    void shouldSetDataFromDraftIfAvailableForEngland() {
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
    void shouldSetDataFromDraftIfAvailableForWales() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .wantToUploadDocuments(VerticalYesNo.YES)
                .build();

        long caseReference = 1234;
        UUID id = UUID.randomUUID();
        DynamicListElement value = DynamicListElement.builder()
                .code(id)
                .label(AdditionalDocumentType.OCCUPATION_LICENCE.getLabel())
                .build();

        Optional<PCSCase> draftCaseData = Optional.of(PCSCase.builder()
                .additionalDocuments(List.of(ListValue.<AdditionalDocument>builder()
                        .value(AdditionalDocument.builder()
                                .documentType(new DynamicList(value,
                                        List.of(new DynamicListElement(
                                                id, AdditionalDocumentType.OCCUPATION_LICENCE.getLabel()))))
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
                .containsExactly(AdditionalDocumentType.OCCUPATION_LICENCE.getLabel());
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
        assertThat(response.getData().getAdditionalDocuments()).isNull();
    }

    
}