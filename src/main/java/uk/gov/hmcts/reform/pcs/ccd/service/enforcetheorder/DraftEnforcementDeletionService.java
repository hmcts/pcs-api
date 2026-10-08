package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@AllArgsConstructor
@Service
public class DraftEnforcementDeletionService {

    private final DraftCaseDataRepository draftCaseDataRepository;

    @Transactional
    public void deleteEnforcementDrafts(long discardDays) {
        Instant cutoff = Instant.now().minus(discardDays, ChronoUnit.DAYS);
        log.info("Running deletion of Enforcement Drafts at a cut off of {}", cutoff);
        int numberRemoved = draftCaseDataRepository
                .deleteByEventIdAndCutoff(EventId.enforceTheOrder, cutoff);
        log.info("Removed: {} cases.", numberRemoved);
    }
}
