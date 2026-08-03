package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Derives which (recipient, document) pairs have already been posted from the {@code PACK_SENT}
 * SUCCESS rows' {@link PackDetails#documents()} — the bulk-print dedup source. A pack's documents
 * are merged into one letter and posted atomically, so pack coverage and per-document coverage are
 * equivalent; a document not covered by any prior pack (e.g. a late counter-claim) goes out on the
 * next sweep without re-sending the rest.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SentPackDocuments {

    private final ObjectMapper objectMapper;

    public Set<String> sentDocumentKeys(List<ClaimActivityLogEntity> activityLog) {
        Set<String> keys = new HashSet<>();
        activityLog.stream()
            .filter(entry -> entry.getActivityType() == ClaimActivityType.PACK_SENT)
            .filter(entry -> entry.getStatus() == ClaimActivityStatus.SUCCESS)
            .filter(entry -> entry.getParty() != null && entry.getDetails() != null)
            .forEach(entry -> addKeys(keys, entry));
        return keys;
    }

    public static String key(UUID partyId, UUID documentId) {
        return partyId + ":" + documentId;
    }

    private void addKeys(Set<String> keys, ClaimActivityLogEntity entry) {
        try {
            PackDetails details = objectMapper.readValue(entry.getDetails(), PackDetails.class);
            if (details.documents() == null) {
                return;
            }
            details.documents().forEach(document -> keys.add(key(entry.getParty().getId(), document.id())));
        } catch (Exception e) {
            // Unreadable details can't say which documents were covered; skipping the row means those
            // documents count as unsent (a re-send is preferable to silently never sending).
            log.error("Unreadable PACK_SENT details on activity row {} - ignoring for dedup", entry.getId(), e);
        }
    }
}
