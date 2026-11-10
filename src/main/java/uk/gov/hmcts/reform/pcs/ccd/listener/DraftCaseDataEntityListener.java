package uk.gov.hmcts.reform.pcs.ccd.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.AttachDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.AttachDocumentTaskComponent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attaches files uploaded into any draft to the draft's case in CDAM, as {@link DocumentEntityListener} does for
 * submitted documents. Unattached, CDAM stops serving them after its one-day TTL.
 *
 * <p>An upload is any document in the draft that carries the hash token CDAM issued for it. Documents copied into
 * a draft from the case have none. The token goes to CDAM, which refuses a document already on another case.
 * Once attached, the user opens the file through the draftUploads case field.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftCaseDataEntityListener {

    private static final Pattern DOCUMENT_ID = Pattern.compile(
        "/documents/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SchedulerClient schedulerClient;

    @PostLoad
    public void onPostLoad(DraftCaseDataEntity draft) {
        draft.setSavedCaseData(draft.getCaseData());
    }

    @PostPersist
    @PostUpdate
    public void onSave(DraftCaseDataEntity draft) {
        if (hasHashToken(draft.getCaseData())) {
            try {
                Map<UUID, String> uploads = uploads(draft.getCaseData());
                uploads.keySet().removeAll(uploads(draft.getSavedCaseData()).keySet());
                if (!uploads.isEmpty()) {
                    long caseReference = draft.getCaseReference();
                    afterCommit(() -> scheduleAll(caseReference, uploads));
                }
            } catch (JsonProcessingException e) {
                // Never fail the draft save: the file stays unattached, as it was before this existed.
                log.error("Could not read draft uploads for case {}", draft.getCaseReference(), e);
            }
        }
        draft.setSavedCaseData(draft.getCaseData());
    }

    // The draft is already saved by now, so a scheduler failure must not be reported to the user as a failed save.
    private void scheduleAll(long caseReference, Map<UUID, String> uploads) {
        try {
            uploads.forEach((documentId, hashToken) -> schedule(caseReference, documentId, hashToken));
        } catch (RuntimeException e) {
            log.error("Could not queue the attach of draft uploads {} to case {}", uploads.keySet(), caseReference, e);
        }
    }

    // The scheduler writes on its own connection, so a task queued here would survive a rolled-back draft save.
    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private static Map<UUID, String> uploads(String caseData) throws JsonProcessingException {
        Map<UUID, String> uploads = new LinkedHashMap<>();
        if (hasHashToken(caseData)) {
            for (JsonNode document : JSON.readTree(caseData).findParents("document_hash")) {
                String hashToken = document.get("document_hash").textValue();
                if (hashToken != null && !hashToken.isBlank()) {
                    Matcher url = DOCUMENT_ID.matcher(document.path("document_url").asText());
                    if (url.find()) {
                        uploads.put(UUID.fromString(url.group(1)), hashToken);
                    }
                }
            }
        }
        return uploads;
    }

    // Most saves hold no upload, so are not worth parsing.
    private static boolean hasHashToken(String caseData) {
        return caseData != null && caseData.contains("document_hash");
    }

    private void schedule(long caseReference, UUID documentId, String hashToken) {
        log.info("Queueing attach of draft upload {} to case {}", documentId, caseReference);
        schedulerClient.scheduleIfNotExists(
            AttachDocumentTaskComponent.ATTACH_DOCUMENT_TASK_DESCRIPTOR
                .instance("draft-" + caseReference + "-" + documentId)
                .data(AttachDocumentTaskData.builder()
                          .caseReference(caseReference)
                          .documentId(documentId)
                          .hashToken(hashToken)
                          .build())
                .scheduledTo(Instant.now())
        );
    }
}
