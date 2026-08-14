package uk.gov.hmcts.reform.pcs.ccd.service.casedeletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseDataDeletionException;
import uk.gov.hmcts.reform.pcs.exception.DocumentDeletionException;
import uk.gov.hmcts.reform.pcs.exception.DraftDataDeletionException;
import uk.gov.hmcts.reform.pcs.exception.PcsCaseDeletionException;

import java.util.List;

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
    public void deleteDocuments(long caseReference) {
        List<DocumentEntity> documents = pcsCaseService.getDocuments(caseReference);
        if (!CollectionUtils.isEmpty(documents)) {
            deleteDocumentsFromCdam(documents, caseReference);
        }
    }

    private void deleteDocumentsFromCdam(List<DocumentEntity> documents, long caseReference) {
        if (!CollectionUtils.isEmpty(documents)) {
            try {
                pcsCaseService.deleteDocumentsFromCdam(documents, caseReference);
            } catch (Exception e) {
                throw new DocumentDeletionException(caseReference);
            }
        }
    }

    @Transactional
    public void deleteCcdCase(long caseReference) {
        try {
            ccdCaseDataDeletionService.deleteCcdCaseData(caseReference);
        } catch (Exception e) {
            throw new CcdCaseDataDeletionException(caseReference);
        }
    }

    @Transactional
    public void deleteDraftData(long caseReference) {
        try {
            draftCaseDataService.deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
        } catch (Exception e) {
            throw new DraftDataDeletionException(caseReference);
        }
    }

    @Transactional
    public void deletePcsCase(long caseReference) {
        try {
            pcsCaseService.deleteCase(caseReference);
        } catch (Exception e) {
            throw new PcsCaseDeletionException(caseReference);
        }
    }
}
