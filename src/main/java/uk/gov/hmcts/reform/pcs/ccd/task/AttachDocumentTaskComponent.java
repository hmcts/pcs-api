package uk.gov.hmcts.reform.pcs.ccd.task;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import feign.FeignException;
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

                try {
                    caseDocumentClientApi.patchDocument(
                        systemUpdateUserTokenProvider.getAuthToken(),
                        authTokenGenerator.generate(),
                        CaseDocumentsMetadata.builder()
                            .caseId(String.valueOf(taskData.getCaseReference()))
                            .caseTypeId(CaseType.getCaseType())
                            .jurisdictionId(CaseType.getJurisdictionId())
                            .documentHashTokens(List.of(
                                DocumentHashToken.builder()
                                    .id(taskData.getDocumentId().toString())
                                    .hashToken(taskData.getHashToken())
                                    .build()))
                            .build()
                    );
                    log.info("Attached {} {} to case {}", describe(taskData), taskData.getDocumentId(),
                             taskData.getCaseReference());
                } catch (FeignException.NotFound e) {
                    if (!isDraftUpload(taskData)) {
                        logAttachFailure(taskData, e);
                        throw e;
                    }
                    // pcs-frontend's removeDraftDocument deleted the file before it was attached; a retry cannot work.
                    log.warn("Draft upload {} no longer exists, so not attached to case {}",
                             taskData.getDocumentId(), taskData.getCaseReference());
                } catch (FeignException e) {
                    logAttachFailure(taskData, e);
                    throw e;
                }

                return new CompletionHandler.OnCompleteRemove<>();
            });
    }

    // Only draft uploads carry the hash token CDAM issued on upload.
    private static boolean isDraftUpload(AttachDocumentTaskData taskData) {
        return taskData.getHashToken() != null;
    }

    private static String describe(AttachDocumentTaskData taskData) {
        return isDraftUpload(taskData) ? "draft upload" : "document";
    }

    // CDAM's status says why: 403 is a hash token CDAM rejected (or a file on another case), 5xx is CDAM itself.
    // The scheduler retries, so this is a warning; it logs the final failure.
    private static void logAttachFailure(AttachDocumentTaskData taskData, FeignException e) {
        log.warn("Could not attach {} {} to case {}: CDAM returned {}", describe(taskData),
                 taskData.getDocumentId(), taskData.getCaseReference(), e.status());
    }
}
