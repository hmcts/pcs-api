package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.DRAFT_DISCARDED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.REQUESTED_FOR_DELETION;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Slf4j
@AllArgsConstructor
@Service
public class DraftEnforcementDeletionService {

    private final DraftCaseDataRepository draftCaseDataRepository;
    private final EnforcementOrderRepository enforcementOrderRepository;

    @Transactional
    public void deleteExpiredDraftEnforcementApplications(long discardDays) {
        deleteFromDraftTable(discardDays);
        deleteFromEnforcementApplicationTable(discardDays);
    }

    @Transactional
    public void deleteDraftEnforcementApplicationsMarkedForDeletion() {
        deleteFromDraftTableByState();
    }

    private void deleteFromDraftTable(long discardDays) {
        Instant cutoff = Instant.now().minus(discardDays, ChronoUnit.DAYS);
        log.info("Running deletion of Enforcement Drafts at a cut off of {}", cutoff);
        int numberRemoved = draftCaseDataRepository
                .deleteByEventIdAndCutoff(enforceTheOrder, cutoff);
        log.info("Removed: {} cases.", numberRemoved);
    }

    private void deleteFromEnforcementApplicationTable(long discardDays) {
        Instant cutoff = Instant.now().minus(discardDays, ChronoUnit.DAYS);
        log.info("Running deletion of Draft Enforcement Applications at a cut off of {}", cutoff);
        int numberRemoved = enforcementOrderRepository.deleteByLastModifiedAtBeforeAndStateIn(
                cutoff,
                List.of(
                        DRAFT_DISCARDED,
                        AWAITING_SUBMISSION_TO_HMCTS,
                        PENDING_CASE_ISSUED
                )
        );
        log.info("Removed: {} draft enforcement applications.", numberRemoved);
    }

    private void deleteFromDraftTableByState() {
        List<Long> caseReferences = findCaseReferencesRequestedForDeletion();
        if (caseReferences.isEmpty()) {
            log.info("No enforcement applications are marked for deletion.");
            return;
        }

        int numberRemoved = draftCaseDataRepository.deleteByEventIdAndCaseReferenceIn(
                enforceTheOrder,
                caseReferences
        );
        log.info("Removed: {} cases from draft table.", numberRemoved);
        int numberUpdated = enforcementOrderRepository.updateStateByCaseReferenceInAndState(
                caseReferences,
                REQUESTED_FOR_DELETION,
                DRAFT_DISCARDED
        );
        log.info("Marked {} enforcement applications as draft discarded.", numberUpdated);
    }

    private List<Long> findCaseReferencesRequestedForDeletion() {
        return enforcementOrderRepository.findCaseReferencesByState(REQUESTED_FOR_DELETION);
    }
}
