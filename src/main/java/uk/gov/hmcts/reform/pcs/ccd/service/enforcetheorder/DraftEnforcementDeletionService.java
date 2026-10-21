package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@AllArgsConstructor
@Service
public class DraftEnforcementDeletionService {

    static final String DISCARD_DAYS_TOO_SMALL =
            "enforcement-draft-deletion.discard-after-days must be at least 1, but was %d";

    private final DraftCaseDataRepository draftCaseDataRepository;
    private final Clock utcClock;

    /**
     * Fewer than one day puts the cutoff at or after now, which deletes every enforcement draft on the
     * environment rather than the expired ones.
     */
    @Transactional
    public void deleteEnforcementDraftsBatch(long discardDays) {
        if (discardDays < 1) {
            throw new IllegalArgumentException(DISCARD_DAYS_TOO_SMALL.formatted(discardDays));
        }
        Instant cutoff = Instant.now(utcClock).minus(discardDays, ChronoUnit.DAYS);
        log.info("Running deletion of Enforcement Drafts at a cut off of {}", cutoff);
        int numberRemoved = draftCaseDataRepository
                .deleteByEventIdAndCutoff(EventId.enforceTheOrder, cutoff);
        log.info("Removed: {} cases.", numberRemoved);
    }
}
