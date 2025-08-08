package uk.gov.hmcts.reform.pcs.ccd.event;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.ccd.sdk.api.callback.Start;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;
import static uk.gov.hmcts.ccd.sdk.api.Permission.D;

@ExtendWith(MockitoExtension.class)
class UploadDocumentPocTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    private Event<PCSCase, UserRole, State> configuredEvent;

    @BeforeEach
    void setUp() {
        UploadDocumentPoc underTest = new UploadDocumentPoc(pcsCaseService);
        configuredEvent = getEvent(EventId.uploadDocumentPoc, buildEventConfig(underTest));
    }

    @Test
    void shouldSetEventPermissions() {
        SetMultimap<UserRole, Permission> grants = configuredEvent.getGrants();
        assertThat(grants.keySet()).hasSize(1);
        assertThat(grants.get(UserRole.PCS_CASE_WORKER)).contains(C, R, U, D);
    }

    @Test
    void shouldInitializeCaseDataOnStart() {
        long caseReference = 1234L;
        PCSCase caseData = PCSCase.builder().build();
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        Start<PCSCase, State> startHandler = configuredEvent.getStartHandler();
        PCSCase result = startHandler.start(eventPayload);

        assertThat(result.getApplicantForename()).isEqualTo("Preset value");
    }

    @Test
    void shouldCreateCaseOnSubmit() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        verify(pcsCaseService).createCase(caseReference, caseData);
    }

    @Test
    void shouldProcessMultipleDocuments() {
        // Given
        long caseReference = 1234L;

        Document document1 = mock(Document.class);
        when(document1.getFilename()).thenReturn("doc1.docx");
        when(document1.getBinaryUrl()).thenReturn("/path/to/doc1.docx");

        Document document2 = mock(Document.class);
        when(document2.getFilename()).thenReturn("doc2.docx");
        when(document2.getBinaryUrl()).thenReturn("/path/to/doc2.docx");

        ListValue<Document> documentWrapper1 = ListValue.<Document>builder()
            .value(document1)
            .build();

        ListValue<Document> documentWrapper2 = ListValue.<Document>builder()
            .value(document2)
            .build();

        List<ListValue<Document>> supportingDocuments = Arrays.asList(documentWrapper1, documentWrapper2);

        PCSCase caseData = mock(PCSCase.class);
        when(caseData.getSupportingDocuments()).thenReturn(supportingDocuments);

        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        // When
        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        // Then - Only document-related verifications
        verify(pcsCaseService).addDocumentToCase(caseReference, "doc1.docx", "/path/to/doc1.docx");
        verify(pcsCaseService).addDocumentToCase(caseReference, "doc2.docx", "/path/to/doc2.docx");
    }

    @Test
    void shouldHandleNullSupportingDocuments() {
        // Given
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        when(caseData.getSupportingDocuments()).thenReturn(null);

        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        // When
        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        // Then - Verify no documents are processed
        verify(pcsCaseService, never()).addDocumentToCase(anyLong(), anyString(), anyString());
    }

    @Test
    void shouldHandleEmptySupportingDocuments() {
        // Given
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);
        when(caseData.getSupportingDocuments()).thenReturn(Collections.emptyList());

        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        // When
        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        // Then - Verify no documents are processed
        verify(pcsCaseService, never()).addDocumentToCase(anyLong(), anyString(), anyString());
    }

    @Test
    void shouldSkipNullDocumentWrapper() {
        // Given
        long caseReference = 1234L;

        Document validDocument = mock(Document.class);
        when(validDocument.getFilename()).thenReturn("valid.docx");
        when(validDocument.getBinaryUrl()).thenReturn("/path/to/valid.docx");

        ListValue<Document> validWrapper = ListValue.<Document>builder()
            .value(validDocument)
            .build();

        // Use null directly in the list
        List<ListValue<Document>> supportingDocuments = Arrays.asList(null, validWrapper);

        PCSCase caseData = mock(PCSCase.class);
        when(caseData.getSupportingDocuments()).thenReturn(supportingDocuments);

        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        // When
        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        // Then - Only the valid document should be processed
        verify(pcsCaseService, times(1)).addDocumentToCase(caseReference, "valid.docx", "/path/to/valid.docx");
    }

    @Test
    void shouldSkipNullDocumentValue() {
        // Given
        long caseReference = 1234L;

        // Create wrapper with null value
        ListValue<Document> nullValueWrapper = ListValue.<Document>builder()
            .value(null)
            .build();

        Document validDocument = mock(Document.class);
        when(validDocument.getFilename()).thenReturn("valid.docx");
        when(validDocument.getBinaryUrl()).thenReturn("/path/to/valid.docx");

        ListValue<Document> validWrapper = ListValue.<Document>builder()
            .value(validDocument)
            .build();

        List<ListValue<Document>> supportingDocuments = Arrays.asList(nullValueWrapper, validWrapper);

        PCSCase caseData = mock(PCSCase.class);
        when(caseData.getSupportingDocuments()).thenReturn(supportingDocuments);

        EventPayload<PCSCase, State> eventPayload = new EventPayload<>(caseReference, caseData, null);

        // When
        Submit<PCSCase, State> submitHandler = configuredEvent.getSubmitHandler();
        submitHandler.submit(eventPayload);

        // Then - Only the valid document should be processed
        verify(pcsCaseService, times(1)).addDocumentToCase(caseReference, "valid.docx", "/path/to/valid.docx");
    }
}
