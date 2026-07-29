package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@Service
@AllArgsConstructor
public class CaseDeletionService {

    private final CcdCaseDataService ccdCaseDataService;
    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;

    @Transactional
    public void deleteCase(long caseReference) {
        ccdCaseDataService.deleteCcdCaseData(caseReference);
        draftCaseDataService.deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
        pcsCaseService.deleteCase(caseReference);
    }
}
