package uk.gov.hmcts.reform.pcs.ccd.service.casedeletion;

import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseNotFoundException;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

/**
 * Deletion feature for data held in decentralised ccd, draft and PUBLIC (PCS) schemas.
 */
@Service
@AllArgsConstructor
@Slf4j
public class CaseDeletionService {

    private final CcdCaseDataDeletionService ccdCaseDataDeletionService;
    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;

    @Transactional
    public void performCaseDeletionTasks(long caseRef) {
        log.debug("Performing case deletion tasks for case: {}", caseRef);
        try {
            ccdCaseDataDeletionService.markCaseForDeletion(caseRef);
            ccdCaseDataDeletionService.confirmCaseDisposal(caseRef);
        } catch (CcdCaseNotFoundException e) {
            log.error("Case not found in main ccd datastore. Will proceed to delete in decentralised ccd schema");
        } catch (FeignException e) {
            log.error("Error occurred while performing case deletion tasks for case: {}. Error: {}",
                    caseRef, e.getMessage());
            throw e;
        }
        deleteDocuments(caseRef);
        deleteCase(caseRef);
    }

    @Transactional
    public void cleanupDiscardedDraftCases(long caseRef) {
        deleteDocuments(caseRef);
        deleteCase(caseRef);
    }

    protected void deleteDocuments(long caseReference) {
        pcsCaseService.deleteDocuments(caseReference);
    }

    protected void deleteCase(long caseReference) {
        ccdCaseDataDeletionService.deleteCcdCaseData(caseReference);
        draftCaseDataService.deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
        pcsCaseService.deleteCase(caseReference);
    }
}
