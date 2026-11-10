package uk.gov.hmcts.reform.pcs.ccd.listener;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.AttachDocumentTaskData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@ExtendWith(MockitoExtension.class)
class DraftCaseDataEntityListenerTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID UPLOAD = UUID.randomUUID();

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private DraftCaseDataEntityListener underTest;

    @Test
    void shouldAttachAnUploadWhereverItIsInANewDraft() {
        DraftCaseDataEntity draft = draft(resumePossessionClaim, """
            {"rentArrears_StatementDocuments": [{"id": "1", "value": %s}]}""".formatted(upload(UPLOAD)));

        underTest.onSave(draft);

        AttachDocumentTaskData data = scheduledData("draft-" + CASE_REFERENCE + "-" + UPLOAD);
        assertThat(data.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(data.getDocumentId()).isEqualTo(UPLOAD);
        assertThat(data.getHashToken()).isEqualTo("hash");
    }

    @Test
    void shouldOnlyAttachUploadsAddedSinceTheDraftWasLoaded() {
        UUID added = UUID.randomUUID();
        DraftCaseDataEntity draft = draft(enforceTheOrder, """
            {"first": %s}""".formatted(upload(UPLOAD)));
        underTest.onPostLoad(draft);
        draft.setCaseData("""
            {"first": %s, "second": %s}""".formatted(upload(UPLOAD), upload(added)));

        underTest.onSave(draft);

        assertThat(scheduledData("draft-" + CASE_REFERENCE + "-" + added).getDocumentId()).isEqualTo(added);
    }

    @Test
    void shouldNotAttachAnUploadAgainOnALaterSaveOfTheSameEntity() {
        DraftCaseDataEntity draft = draft(resumePossessionClaim, """
            {"first": %s}""".formatted(upload(UPLOAD)));
        underTest.onSave(draft);

        underTest.onSave(draft);

        verify(schedulerClient).scheduleIfNotExists(any());
    }

    @Test
    void shouldNotAttachDocumentsWithoutAHashTokenAsTheyWereCopiedFromTheCase() {
        underTest.onSave(draft(enforceTheOrder, """
            {"allDocuments": [{"value": {"document_url": "http://dm-store/documents/%s"}}]}""".formatted(UPLOAD)));

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldNotAttachRespondToClaimUploadsAsTheCaseViewDoesNotReturnThem() {
        underTest.onSave(draft(respondPossessionClaim, """
            {"first": %s}""".formatted(upload(UPLOAD))));

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldNotFailTheSaveWhenTheDraftCannotBeRead() {
        assertThatCode(() -> underTest.onSave(draft(resumePossessionClaim, "not json")))
            .doesNotThrowAnyException();

        verifyNoInteractions(schedulerClient);
    }

    @SuppressWarnings("unchecked")
    private AttachDocumentTaskData scheduledData(String expectedInstanceId) {
        ArgumentCaptor<SchedulableInstance<AttachDocumentTaskData>> captor =
            ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());
        assertThat(captor.getValue().getTaskInstance().getId()).isEqualTo(expectedInstanceId);
        return captor.getValue().getTaskInstance().getData();
    }

    private static String upload(UUID documentId) {
        return """
            {"document_url": "http://dm-store/documents/%s", "document_hash": "hash"}""".formatted(documentId);
    }

    private static DraftCaseDataEntity draft(EventId eventId, String caseData) {
        DraftCaseDataEntity draft = new DraftCaseDataEntity();
        draft.setCaseReference(CASE_REFERENCE);
        draft.setEventId(eventId);
        draft.setCaseData(caseData);
        return draft;
    }
}
