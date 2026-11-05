package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@AllArgsConstructor
@Slf4j
public class DraftResponseDeletionService {

    private final DraftCaseDataRepository draftCaseDataRepository;

    @Transactional
    public void deleteRespondPossessionClaimBatch(long discardDays) {
        Instant cutoff = Instant.now().minus(discardDays, ChronoUnit.DAYS);
        log.info("Running deletion of Respond Possession Claim at a cut off of {}", cutoff);
        int numberRemoved = draftCaseDataRepository
            .deleteByEventIdAndCutoff(EventId.respondPossessionClaim, cutoff);
        log.info("Removed: {} cases.", numberRemoved);
    }

}
