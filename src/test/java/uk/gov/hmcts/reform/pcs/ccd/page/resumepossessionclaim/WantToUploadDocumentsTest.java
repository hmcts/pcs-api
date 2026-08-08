package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.Optional;

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
    void shouldNotSetAdditionalDocumentsIfUserSelectsNoForEngland() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.ENGLAND)
                .wantToUploadDocuments(VerticalYesNo.NO)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getAdditionalDocumentsEngland()).isNull();
    }

    @Test
    void shouldNotSetAdditionalDocumentsIfUserSelectsNoForWales() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .wantToUploadDocuments(VerticalYesNo.NO)
                .build();

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getAdditionalDocumentsWales()).isNull();
    }

    @Test
    void shouldSetDataFromDraftIfAvailableForEngland() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.ENGLAND)
                .wantToUploadDocuments(VerticalYesNo.YES)
                .build();

        long caseReference = 1234;
        
        AdditionalDocumentEngland additionalDocument = AdditionalDocumentEngland.builder()
            .documentType(AdditionalDocumentTypeEngland.TENANCY_AGREEMENT)
            .description("Tenancy agreement")
            .build();

        Optional<PCSCase> draftCaseData = Optional.of(PCSCase.builder()
                .additionalDocumentsEngland(List.of(
                    ListValue.<AdditionalDocumentEngland>builder()
                        .value(additionalDocument)
                        .build()))
                .build());

        when(draftCaseDataService.getUnsubmittedCaseData(caseReference, EventId.resumePossessionClaim))
                .thenReturn(draftCaseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        AdditionalDocumentEngland addDoc = response.getData().getAdditionalDocumentsEngland().getFirst().getValue();
        assertThat(addDoc.getDescription()).isEqualTo("Tenancy agreement");
        assertThat(addDoc.getDocumentType()).isEqualTo(AdditionalDocumentTypeEngland.TENANCY_AGREEMENT);

    }

    @Test
    void shouldSetDataFromDraftIfAvailableForWales() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .wantToUploadDocuments(VerticalYesNo.YES)
                .build();

        long caseReference = 1234;
        
        AdditionalDocumentWales additionalDocument = AdditionalDocumentWales.builder()
            .documentType(AdditionalDocumentTypeWales.OCCUPATION_LICENCE)
            .description("Occupation contract or licence")
            .build();

        Optional<PCSCase> draftCaseData = Optional.of(PCSCase.builder()
                .additionalDocumentsWales(List.of(
                    ListValue.<AdditionalDocumentWales>builder()
                        .value(additionalDocument)
                        .build()))
                .build());

        when(draftCaseDataService.getUnsubmittedCaseData(caseReference, EventId.resumePossessionClaim))
                .thenReturn(draftCaseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        AdditionalDocumentWales addDoc = response.getData().getAdditionalDocumentsWales().getFirst().getValue();
        assertThat(addDoc.getDescription()).isEqualTo("Occupation contract or licence");
        assertThat(addDoc.getDocumentType()).isEqualTo(AdditionalDocumentTypeWales.OCCUPATION_LICENCE);

    }
    
    @Test
    void shouldSetAdditionalDocumentsToNullIfNotInDraftDataForEngland() {
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
        assertThat(response.getData().getAdditionalDocumentsEngland()).isNull();
    }

    @Test
    void shouldSetAdditionalDocumentsToNullIfNotInDraftDataForWales() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .legislativeCountry(LegislativeCountry.WALES)
                .wantToUploadDocuments(VerticalYesNo.YES)
                .build();

        long caseReference = 1234;

        Optional<PCSCase> draftCaseData = Optional.of(PCSCase.builder().build());

        when(draftCaseDataService.getUnsubmittedCaseData(caseReference, EventId.resumePossessionClaim))
                .thenReturn(draftCaseData);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getData().getAdditionalDocumentsWales()).isNull();
    }

    @Test
    void shouldMapAdditionalDocumentsToAdditionalDocumentsEnglandDocumentsWhenAdditionalDocumentsIsPresent() {
        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .wantToUploadDocuments(VerticalYesNo.YES)
            .build();

        long caseReference = 1234;

        AdditionalDocument legacyAdditionalDocument = AdditionalDocument.builder()
            .document(Document.builder().filename("tenancy-agreement.pdf").build())
            .documentType(DynamicList.builder()
                .value(DynamicListElement.builder().label("Tenancy agreement").build())
                .build())
            .description("Legacy tenancy agreement")
            .build();

        Optional<PCSCase> draftCaseData = Optional.of(PCSCase.builder()
            .additionalDocuments(List.of(
                ListValue.<AdditionalDocument>builder()
                    .value(legacyAdditionalDocument)
                    .build()))
            .build());

        when(draftCaseDataService.getUnsubmittedCaseData(caseReference, EventId.resumePossessionClaim))
            .thenReturn(draftCaseData);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        AdditionalDocumentEngland addDoc = response.getData().getAdditionalDocumentsEngland().getFirst().getValue();
        assertThat(addDoc.getDescription()).isEqualTo("Legacy tenancy agreement");
        assertThat(addDoc.getDocument().getFilename()).isEqualTo("tenancy-agreement.pdf");
        assertThat(addDoc.getDocumentType()).isEqualTo(AdditionalDocumentTypeEngland.TENANCY_AGREEMENT);
    }

    @Test
    void shouldMapAdditionalDocumentsToAdditionalDocumentsWalesDocumentsWhenAdditionalDocumentsIsPresent() {
        PCSCase caseData = PCSCase.builder()
            .legislativeCountry(LegislativeCountry.WALES)
            .wantToUploadDocuments(VerticalYesNo.YES)
            .build();

        long caseReference = 1234;

        AdditionalDocument legacyAdditionalDocument = AdditionalDocument.builder()
            .document(Document.builder().filename("occupation-licence.pdf").build())
            .documentType(DynamicList.builder()
                .value(DynamicListElement.builder().label("Occupation contract or licence").build())
                .build())
            .description("Legacy occupation contract")
            .build();

        Optional<PCSCase> draftCaseData = Optional.of(PCSCase.builder()
            .additionalDocuments(List.of(
                ListValue.<AdditionalDocument>builder()
                    .value(legacyAdditionalDocument)
                    .build()))
            .build());

        when(draftCaseDataService.getUnsubmittedCaseData(caseReference, EventId.resumePossessionClaim))
            .thenReturn(draftCaseData);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        AdditionalDocumentWales addDoc = response.getData().getAdditionalDocumentsWales().getFirst().getValue();
        assertThat(addDoc.getDescription()).isEqualTo("Legacy occupation contract");
        assertThat(addDoc.getDocument().getFilename()).isEqualTo("occupation-licence.pdf");
        assertThat(addDoc.getDocumentType()).isEqualTo(AdditionalDocumentTypeWales.OCCUPATION_LICENCE);
    }
}
