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
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.AttachDocumentTaskData;
import uk.gov.hmcts.reform.pcs.ccd.task.AttachDocumentTaskComponent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

/**
 * Attaches files uploaded into a draft to the draft's case in CDAM, as {@link DocumentEntityListener} does for
 * submitted documents. Unattached, CDAM stops serving them after its one-day TTL.
 *
 * <p>An upload is any document in the draft that carries the hash token CDAM issued for it. Documents copied into
 * a draft from the case have none. The token goes to CDAM, which refuses a document already on another case.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftCaseDataEntityListener {

    // Respond to claim is left out until the case view returns its uploads: attached but not in the view, CDAM
    // answers 404.
    private static final Set<EventId> EVENTS = Set.of(resumePossessionClaim, enforceTheOrder);

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SchedulerClient schedulerClient;

    @PostLoad
    public void onPostLoad(DraftCaseDataEntity draft) {
        draft.setSavedCaseData(draft.getCaseData());
    }

    @PostPersist
    @PostUpdate
    public void onSave(DraftCaseDataEntity draft) {
        if (EVENTS.contains(draft.getEventId())) {
            try {
                Map<UUID, String> uploads = uploads(draft.getCaseData());
                uploads.keySet().removeAll(uploads(draft.getSavedCaseData()).keySet());
                uploads.forEach((documentId, hashToken) -> schedule(draft.getCaseReference(), documentId, hashToken));
            } catch (JsonProcessingException e) {
                // Never fail the draft save: the file stays unattached, as it was before this existed.
                log.error("Could not read draft uploads for case {}", draft.getCaseReference(), e);
            }
        }
        draft.setSavedCaseData(draft.getCaseData());
    }

    private static Map<UUID, String> uploads(String caseData) throws JsonProcessingException {
        Map<UUID, String> uploads = new LinkedHashMap<>();
        if (caseData != null) {
            for (JsonNode document : JSON.readTree(caseData).findParents("document_hash")) {
                DocumentEntityListener.dmStoreDocumentId(document.path("document_url").asText())
                    .ifPresent(documentId -> uploads.put(documentId, document.get("document_hash").asText()));
            }
        }
        return uploads;
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
