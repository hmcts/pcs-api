package uk.gov.hmcts.reform.pcs.ccd.event;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.EventTypeBuilder;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.testingsupport.bulkprint.TestBulkPrintTaskComponent;
import uk.gov.hmcts.reform.pcs.testingsupport.bulkprint.TestBulkPrintTaskData;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.testBulkPrint;
import static uk.gov.hmcts.reform.pcs.ccd.event.TestBulkPrint.EVENT_NAME;

@ExtendWith(MockitoExtension.class)
class TestBulkPrintTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private SchedulerClient schedulerClient;

    @Test
    @SuppressWarnings("unchecked")
    void shouldConfigureTheEventForEveryStateWithAnUploadPage() {
        DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder = mock(DecentralisedConfigBuilder.class);
        EventTypeBuilder<PCSCase, UserRole, State> typeBuilder = mock(EventTypeBuilder.class);
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = mock(Event.EventBuilder.class, RETURNS_SELF);
        FieldCollection.FieldCollectionBuilder<PCSCase, State, Event.EventBuilder<PCSCase, UserRole, State>> fields =
            mock(FieldCollection.FieldCollectionBuilder.class, RETURNS_SELF);
        doReturn(typeBuilder).when(configBuilder).decentralisedEvent(anyString(), any());
        when(typeBuilder.forAllStates()).thenReturn(eventBuilder);
        when(eventBuilder.fields()).thenReturn(fields);

        underTest().configure(configBuilder);

        verify(configBuilder).decentralisedEvent(eq(testBulkPrint.name()), any());
        verify(eventBuilder).name(EVENT_NAME);
        verify(eventBuilder).grant(Permission.CRU, UserRole.PCS_SOLICITOR);
        verify(eventBuilder).grant(Permission.CRU, UserRole.GA_CLAIMANT_SOLICITOR);
        verify(fields).mandatory(any());
    }

    @Test
    void shouldScheduleALetterOfTheUploadedDocuments() {
        UUID photo = UUID.randomUUID();
        UUID statement = UUID.randomUUID();
        PCSCase caseData = PCSCase.builder().testBulkPrintDocuments(List.of(
            upload(photo, "photo.jpg"), upload(statement, "statement.docx"))).build();

        SubmitResponse<State> response = underTest().submit(payload(caseData));

        TestBulkPrintTaskData scheduled = scheduledTask();
        assertThat(scheduled.caseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(scheduled.documents()).containsExactly(
            new TestBulkPrintTaskData.Upload(photo, "photo.jpg"),
            new TestBulkPrintTaskData.Upload(statement, "statement.docx"));
        assertThat(response.getConfirmationBody()).contains("2 document(s)");
    }

    @Test
    void shouldScheduleAnEmptyLetterWhenNothingWasUploaded() {
        underTest().submit(payload(PCSCase.builder().build()));

        assertThat(scheduledTask().documents()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private TestBulkPrintTaskData scheduledTask() {
        ArgumentCaptor<SchedulableInstance<TestBulkPrintTaskData>> captor =
            ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());
        SchedulableInstance<TestBulkPrintTaskData> instance = captor.getValue();
        assertThat(instance.getTaskName()).isEqualTo(TestBulkPrintTaskComponent.TEST_BULK_PRINT_TASK_NAME);
        return instance.getTaskInstance().getData();
    }

    private TestBulkPrint underTest() {
        return new TestBulkPrint(schedulerClient, new DocumentIdExtractor());
    }

    private static EventPayload<PCSCase, State> payload(PCSCase caseData) {
        return new EventPayload<>(CASE_REFERENCE, caseData, null);
    }

    private static ListValue<Document> upload(UUID documentId, String fileName) {
        return ListValue.<Document>builder()
            .value(Document.builder()
                .url("http://dm-store/documents/" + documentId)
                .filename(fileName)
                .build())
            .build();
    }
}
