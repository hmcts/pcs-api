package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentHashToken;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.model.AttachDocumentTaskData;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class AttachDocumentTaskComponent {

    private static final String ATTACH_DOCUMENT_TASK_NAME = "attach-document-task";

    public static final TaskDescriptor<AttachDocumentTaskData> ATTACH_DOCUMENT_TASK_DESCRIPTOR =
        TaskDescriptor.of(ATTACH_DOCUMENT_TASK_NAME, AttachDocumentTaskData.class);

    private final CaseDocumentClientApi caseDocumentClientApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final int maxRetries;
    private final Duration backoffDelay;

    public AttachDocumentTaskComponent(
        CaseDocumentClientApi caseDocumentClientApi,
        AuthTokenGenerator authTokenGenerator,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        @Value("${attach-document.max-retries}") int maxRetries,
        @Value("${attach-document.backoff-delay-seconds}") Duration backoffDelay
    ) {
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.authTokenGenerator = authTokenGenerator;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
    }

    @Bean
    public CustomTask<AttachDocumentTaskData> attachDocumentTask() {
        return Tasks.custom(ATTACH_DOCUMENT_TASK_DESCRIPTOR)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                maxRetries,
                new FailureHandler.ExponentialBackoffFailureHandler<>(backoffDelay)
            ))
            .execute((taskInstance, executionContext) -> {
                AttachDocumentTaskData taskData = taskInstance.getData();
                log.info("Attaching document {} to case {}", taskData.getDocumentId(), taskData.getCaseReference());

                caseDocumentClientApi.patchDocument(
                    systemUpdateUserTokenProvider.getAuthToken(),
                    authTokenGenerator.generate(),
                    CaseDocumentsMetadata.builder()
                        .caseId(String.valueOf(taskData.getCaseReference()))
                        .caseTypeId(CaseType.getCaseType())
                        .jurisdictionId(CaseType.getJurisdictionId())
                        .documentHashTokens(List.of(
                            DocumentHashToken.builder().id(taskData.getDocumentId().toString()).build()))
                        .build()
                );

                return new CompletionHandler.OnCompleteRemove<>();
            });
    }
}
