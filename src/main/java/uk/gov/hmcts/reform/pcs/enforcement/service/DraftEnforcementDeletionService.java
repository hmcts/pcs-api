package uk.gov.hmcts.reform.pcs.enforcement.service;

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
     * Deletes every enforcement draft last saved more than {@code discardDays} days ago.
     *
     * @param discardDays the draft age, in days, at which a draft is discarded; at least 1
     * @throws IllegalArgumentException if {@code discardDays} is less than 1, which would put the cutoff at
     *                                  or after the current time and so delete every enforcement draft
     *                                  regardless of age
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
        log.info("Removed {} enforcement drafts last saved before {}", numberRemoved, cutoff);
    }
}
