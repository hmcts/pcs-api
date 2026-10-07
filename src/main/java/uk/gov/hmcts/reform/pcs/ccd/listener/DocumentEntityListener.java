package uk.gov.hmcts.reform.pcs.ccd.listener;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import jakarta.persistence.PostPersist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.model.AttachDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.AttachDocumentTaskComponent;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attaches each document PCS keeps to its case in CDAM. CDAM only serves a document that is on no case until its TTL
 * passes (a day), and PCS keeps its documents in its own tables, so the data store never attaches them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEntityListener {

    private static final Pattern DOCUMENT_ID = Pattern.compile(
        "/documents/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    private final SchedulerClient schedulerClient;

    @PostPersist
    public void onPostPersist(DocumentEntity entity) {
        // Warrant of restitution documents are saved before they are linked to anything, so have no case yet
        if (entity.getPcsCase() == null) {
            return;
        }

        Optional<UUID> documentId = dmStoreDocumentId(entity);
        if (documentId.isEmpty()) {
            log.warn("Not attaching document {} to its case, as its URL has no DM Store document id", entity.getId());
            return;
        }

        schedulerClient.scheduleIfNotExists(
            AttachDocumentTaskComponent.ATTACH_DOCUMENT_TASK_DESCRIPTOR
                .instance(entity.getId().toString())
                .data(AttachDocumentTaskData.builder()
                          .caseReference(entity.getPcsCase().getCaseReference())
                          .documentId(documentId.get())
                          .build())
                .scheduledTo(Instant.now())
        );
    }

    private static Optional<UUID> dmStoreDocumentId(DocumentEntity entity) {
        Matcher matcher = DOCUMENT_ID.matcher(String.valueOf(entity.getUrl()));
        return matcher.find() ? Optional.of(UUID.fromString(matcher.group(1))) : Optional.empty();
    }
}
