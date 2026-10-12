package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.model.AttachDocumentTaskData;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.task.AttachDocumentTaskComponent.ATTACH_DOCUMENT_TASK_DESCRIPTOR;

@ExtendWith(MockitoExtension.class)
class AttachDocumentTaskComponentTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private TaskInstance<AttachDocumentTaskData> taskInstance;
    @Mock
    private ExecutionContext executionContext;

    private AttachDocumentTaskComponent underTest;

    @BeforeEach
    void setUp() {
        underTest = new AttachDocumentTaskComponent(caseDocumentClientApi, authTokenGenerator,
                                                    systemUpdateUserTokenProvider, 3, Duration.ofSeconds(10));
        when(taskInstance.getData()).thenReturn(AttachDocumentTaskData.builder()
                                                    .caseReference(CASE_REFERENCE)
                                                    .documentId(DOCUMENT_ID)
                                                    .build());
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("user token");
        when(authTokenGenerator.generate()).thenReturn("s2s token");
    }

    @Test
    void shouldAttachTheDocumentToItsCase() {
        CustomTask<AttachDocumentTaskData> task = underTest.attachDocumentTask();
        CompletionHandler<AttachDocumentTaskData> completionHandler = task.execute(taskInstance, executionContext);

        assertThat(completionHandler).isInstanceOf(CompletionHandler.OnCompleteRemove.class);
        assertThat(ATTACH_DOCUMENT_TASK_DESCRIPTOR.getTaskName()).isEqualTo("attach-document-task");

        ArgumentCaptor<CaseDocumentsMetadata> metadata = ArgumentCaptor.forClass(CaseDocumentsMetadata.class);
        verify(caseDocumentClientApi).patchDocument(eq("user token"),
                                                    eq("s2s token"),
                                                    metadata.capture());
        assertThat(metadata.getValue().getCaseId()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(metadata.getValue().getCaseTypeId()).isEqualTo(CaseType.getCaseType());
        assertThat(metadata.getValue().getJurisdictionId()).isEqualTo(CaseType.getJurisdictionId());
        assertThat(metadata.getValue().getDocumentHashTokens())
            .singleElement()
            .satisfies(token -> {
                assertThat(token.getId()).isEqualTo(DOCUMENT_ID.toString());
                assertThat(token.getHashToken()).isNull();
            });
    }

    @Test
    void shouldRethrowWhenCdamFails() {
        when(caseDocumentClientApi.patchDocument(anyString(), anyString(), any(CaseDocumentsMetadata.class)))
            .thenThrow(new RuntimeException("CDAM unavailable"));

        CustomTask<AttachDocumentTaskData> task = underTest.attachDocumentTask();

        assertThatThrownBy(() -> task.execute(taskInstance, executionContext))
            .hasMessage("CDAM unavailable");
    }
}
