package uk.gov.hmcts.reform.pcs.ccd.service.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.AttachDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.AttachDocumentTaskComponent;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attaches files uploaded into a draft to the draft's case in CDAM. Unattached, CDAM stops serving them after its
 * one-day TTL; once attached, it serves them to any user who can see them in the case.
 *
 * <p>Runs in the draft write's transaction: db-scheduler joins it, so the task commits or rolls back with the
 * draft.
 */
@Slf4j
@Component
public class DraftUploadAttacher {

    private static final Pattern DOCUMENT_ID = Pattern.compile(
        "/documents/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    private final SchedulerClient schedulerClient;
    private final ObjectMapper draftObjectMapper;

    public DraftUploadAttacher(SchedulerClient schedulerClient,
                               @Qualifier("draftCaseDataObjectMapper") ObjectMapper draftObjectMapper) {
        this.schedulerClient = schedulerClient;
        this.draftObjectMapper = draftObjectMapper;
    }

    /** Queues an attach for each upload in the saved draft that was not in it before. */
    public void attachNewUploads(long caseReference, EventId eventId, String draftBefore, String draftAfter) {
        try {
            Set<UUID> added = uploadIds(eventId, draftAfter);
            added.removeAll(uploadIds(eventId, draftBefore));
            added.forEach(documentId -> schedule(caseReference, documentId));
        } catch (JsonProcessingException e) {
            // Never fail the draft save: the file stays unattached, as it was before this existed.
            log.error("Could not read draft uploads for case {}, event {}", caseReference, eventId, e);
        }
    }

    private Set<UUID> uploadIds(EventId eventId, String draftJson) throws JsonProcessingException {
        Set<UUID> ids = new LinkedHashSet<>();
        if (draftJson == null) {
            return ids;
        }
        PCSCase draft = draftObjectMapper.readValue(draftJson, PCSCase.class);
        for (Document document : DraftUploads.forEvent(eventId, draft)) {
            documentId(document.getUrl()).ifPresent(ids::add);
        }
        return ids;
    }

    private void schedule(long caseReference, UUID documentId) {
        log.info("Queueing attach of draft upload {} to case {}", documentId, caseReference);
        schedulerClient.scheduleIfNotExists(
            AttachDocumentTaskComponent.ATTACH_DOCUMENT_TASK_DESCRIPTOR
                .instance("draft-" + caseReference + "-" + documentId)
                .data(AttachDocumentTaskData.builder()
                          .caseReference(caseReference)
                          .documentId(documentId)
                          .build())
                .scheduledTo(Instant.now())
        );
    }

    static Optional<UUID> documentId(String url) {
        Matcher matcher = DOCUMENT_ID.matcher(String.valueOf(url));
        return matcher.find() ? Optional.of(UUID.fromString(matcher.group(1))) : Optional.empty();
    }
}
