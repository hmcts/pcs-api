package uk.gov.hmcts.reform.pcs.ccd.service.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.AttachDocumentTaskData;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftUploadAttacherTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID KEPT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADDED = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private ObjectMapper draftObjectMapper;

    private DraftUploadAttacher underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftUploadAttacher(schedulerClient, draftObjectMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueueAnAttachForEachUploadNewInTheDraft() throws JsonProcessingException {
        when(draftObjectMapper.readValue("before", PCSCase.class)).thenReturn(draftWith(KEPT));
        when(draftObjectMapper.readValue("after", PCSCase.class)).thenReturn(draftWith(KEPT, ADDED));

        underTest.attachNewUploads(CASE_REFERENCE, EventId.resumePossessionClaim, "before", "after");

        ArgumentCaptor<SchedulableInstance<AttachDocumentTaskData>> captor =
            ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());
        assertThat(captor.getAllValues()).singleElement().satisfies(instance -> {
            assertThat(instance.getTaskInstance().getId()).isEqualTo("draft-" + CASE_REFERENCE + "-" + ADDED);
            assertThat(instance.getTaskInstance().getData().getCaseReference()).isEqualTo(CASE_REFERENCE);
            assertThat(instance.getTaskInstance().getData().getDocumentId()).isEqualTo(ADDED);
        });
    }

    @Test
    void shouldQueueEveryUploadInANewDraft() throws JsonProcessingException {
        when(draftObjectMapper.readValue("after", PCSCase.class)).thenReturn(draftWith(KEPT, ADDED));

        underTest.attachNewUploads(CASE_REFERENCE, EventId.resumePossessionClaim, null, "after");

        verify(schedulerClient, times(2)).scheduleIfNotExists(any());
    }

    @Test
    void shouldQueueNothingWhenNoUploadWasAdded() throws JsonProcessingException {
        when(draftObjectMapper.readValue("before", PCSCase.class)).thenReturn(draftWith(KEPT, ADDED));
        when(draftObjectMapper.readValue("after", PCSCase.class)).thenReturn(draftWith(KEPT));

        underTest.attachNewUploads(CASE_REFERENCE, EventId.resumePossessionClaim, "before", "after");

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldNotFailTheDraftSaveWhenTheDraftCannotBeRead() throws JsonProcessingException {
        when(draftObjectMapper.readValue("after", PCSCase.class))
            .thenThrow(new JsonProcessingException("bad json") { });

        underTest.attachNewUploads(CASE_REFERENCE, EventId.resumePossessionClaim, null, "after");

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldReadTheDocumentIdFromADocumentUrl() {
        assertThat(DraftUploadAttacher.documentId("http://dm-store:8080/documents/" + ADDED)).contains(ADDED);
        assertThat(DraftUploadAttacher.documentId("http://dm-store:8080/documents/" + ADDED + "/binary"))
            .contains(ADDED);
        assertThat(DraftUploadAttacher.documentId("http://dm-store:8080/not-a-document")).isEmpty();
        assertThat(DraftUploadAttacher.documentId(null)).isEmpty();
    }

    private static PCSCase draftWith(UUID... documentIds) {
        List<ListValue<Document>> documents = Arrays.stream(documentIds)
            .map(id -> ListValue.<Document>builder()
                .value(Document.builder().url("http://dm-store:8080/documents/" + id).build())
                .build())
            .toList();
        return PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder().tenancyLicenceDocuments(documents).build())
            .build();
    }
}
