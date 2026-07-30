package uk.gov.hmcts.reform.pcs.ccd.service.casedeletion;

import lombok.AllArgsConstructor;
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
public class CaseDeletionService {

    private final CcdCaseDataDeletionService ccdCaseDataDeletionService;
    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;

    @Transactional
    public void deleteCase(long caseReference) {
        ccdCaseDataDeletionService.deleteCcdCaseData(caseReference);
        draftCaseDataService.deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
        pcsCaseService.deleteCase(caseReference);
    }
}
