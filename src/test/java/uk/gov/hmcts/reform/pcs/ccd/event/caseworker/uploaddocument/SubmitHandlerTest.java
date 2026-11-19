package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.uploaddocument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocument;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService;
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitHandlerTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private CaseworkerDocumentService caseworkerDocumentService;
    @Mock
    private ConfirmationPanelBuilder confirmationPanelBuilder;

    private SubmitHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitHandler(caseworkerDocumentService, confirmationPanelBuilder);
    }

    @Test
    void shouldSaveDocument() {
        CaseworkerDocument caseworkerDocument = mock(CaseworkerDocument.class);

        DocumentEntity documentEntity = DocumentEntity.builder().build();
        when(caseworkerDocumentService.saveNewDocument(caseworkerDocument, CASE_REFERENCE))
            .thenReturn(documentEntity);

        PCSCase caseData = PCSCase.builder()
            .caseworkerDocument(caseworkerDocument)
            .build();

        // When
        underTest.submit(toEventPayload(caseData));

        // Then
        verify(caseworkerDocumentService).saveNewDocument(caseworkerDocument, CASE_REFERENCE);
    }

    @Test
    void shouldReturnConfirmationBody() {
        CaseworkerDocument caseworkerDocument = mock(CaseworkerDocument.class);

        AddressUK propertyAddresss = mock(AddressUK.class);

        String caseName = "test case name";
        PCSCase caseData = PCSCase.builder()
            .caseNameHmctsInternal(caseName)
            .propertyAddress(propertyAddresss)
            .caseworkerDocument(caseworkerDocument)
            .build();

        String baseFilename = "test document filename";
        DocumentEntity documentEntity = DocumentEntity.builder()
            .fileName(baseFilename + ".pdf")
            .build();

        when(caseworkerDocumentService.saveNewDocument(caseworkerDocument, CASE_REFERENCE))
            .thenReturn(documentEntity);

        String confirmationPanelMarkup = stubConfirmationPanelBuilder();

        // When
        SubmitResponse<State> submitResponse = underTest.submit(toEventPayload(caseData));

        // Then
        String expectedTitle = "‘%s’ uploaded".formatted(baseFilename);
        verify(confirmationPanelBuilder)
            .buildStandardConfirmationPanel(expectedTitle, caseData, CASE_REFERENCE);

        String confirmationBody = submitResponse.getConfirmationBody();
        assertThat(confirmationBody)
            .contains(confirmationPanelMarkup);
    }

    private static EventPayload<PCSCase, State> toEventPayload(PCSCase caseData) {
        return new EventPayload<>(CASE_REFERENCE, caseData, null);
    }

    private String stubConfirmationPanelBuilder() {
        String confirmationPanelMarkup = "confirmation panel markup";
        when(confirmationPanelBuilder
                 .buildStandardConfirmationPanel(anyString(), any(PCSCase.class), anyLong()))
            .thenReturn(confirmationPanelMarkup);

        return confirmationPanelMarkup;
    }

}
