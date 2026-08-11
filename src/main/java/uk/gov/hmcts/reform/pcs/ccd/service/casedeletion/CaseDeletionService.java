package uk.gov.hmcts.reform.pcs.ccd.service.casedeletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

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

    public void deleteDocuments(long caseReference) {
        pcsCaseService.deleteDocuments(caseReference);
    }

    @Transactional(timeout = 15)
    public void deleteCase(long caseReference) {
        ccdCaseDataDeletionService.deleteCcdCaseData(caseReference);
        draftCaseDataService.deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
        pcsCaseService.deleteCase(caseReference);
    }
}
