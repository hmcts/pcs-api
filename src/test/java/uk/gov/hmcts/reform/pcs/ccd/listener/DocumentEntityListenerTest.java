package uk.gov.hmcts.reform.pcs.ccd.listener;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.AttachDocumentTaskData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DocumentEntityListenerTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private SchedulerClient schedulerClient;

    @InjectMocks
    private DocumentEntityListener underTest;

    @Test
    void shouldScheduleAttachUsingTheDocumentIdFromTheUrl() {
        UUID dmStoreId = UUID.randomUUID();
        DocumentEntity document = document(DocumentType.CLAIM, "http://dm-store/documents/" + dmStoreId);

        underTest.onPostPersist(document);

        AttachDocumentTaskData data = scheduledData(document);
        assertThat(data.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(data.getDocumentId()).isEqualTo(dmStoreId);
    }

    @Test
    void shouldNotAttachAccessCodeLetters() {
        underTest.onPostPersist(document(DocumentType.DEFENDANT_ACCESS_CODE,
                                         "http://dm-store/documents/" + UUID.randomUUID()));

        verifyNoInteractions(schedulerClient);
    }

    @Test
    void shouldNotAttachDocumentsWithoutADmStoreId() {
        underTest.onPostPersist(document(DocumentType.CLAIM, "not a dm store url"));

        verifyNoInteractions(schedulerClient);
    }

    @SuppressWarnings("unchecked")
    private AttachDocumentTaskData scheduledData(DocumentEntity document) {
        ArgumentCaptor<SchedulableInstance<AttachDocumentTaskData>> captor =
            ArgumentCaptor.forClass(SchedulableInstance.class);
        verify(schedulerClient).scheduleIfNotExists(captor.capture());
        assertThat(captor.getValue().getTaskInstance().getId()).isEqualTo(document.getId().toString());
        return captor.getValue().getTaskInstance().getData();
    }

    private static DocumentEntity document(DocumentType type, String url) {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setCaseReference(CASE_REFERENCE);
        return DocumentEntity.builder()
            .id(UUID.randomUUID())
            .pcsCase(pcsCase)
            .type(type)
            .url(url)
            .build();
    }
}
