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
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@ExtendWith(MockitoExtension.class)
class SubmitHandlerTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private CaseworkerDocumentService caseworkerDocumentService;
    @Mock
    private AddressFormatter addressFormatter;

    private SubmitHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitHandler(caseworkerDocumentService, addressFormatter);
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

        String formattedAddress = "test formatted address";
        when(addressFormatter.formatMediumAddress(propertyAddresss, COMMA_DELIMITER)).thenReturn(formattedAddress);

        String baseFilename = "test document filename";
        DocumentEntity documentEntity = DocumentEntity.builder()
            .fileName(baseFilename + ".pdf")
            .build();

        when(caseworkerDocumentService.saveNewDocument(caseworkerDocument, CASE_REFERENCE))
            .thenReturn(documentEntity);


        // When
        SubmitResponse<State> submitResponse = underTest.submit(toEventPayload(caseData));

        // Then
        String confirmationBody = submitResponse.getConfirmationBody();
        assertThat(confirmationBody)
            .contains("‘%s’ uploaded".formatted(baseFilename))
            .contains("Case number #%s".formatted(CASE_REFERENCE))
            .contains(formattedAddress)
            .contains(caseName);

    }

    private static EventPayload<PCSCase, State> toEventPayload(PCSCase caseData) {
        return new EventPayload<>(CASE_REFERENCE, caseData, null);
    }

}
