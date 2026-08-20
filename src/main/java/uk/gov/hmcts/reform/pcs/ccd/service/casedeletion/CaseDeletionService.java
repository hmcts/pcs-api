package uk.gov.hmcts.reform.pcs.ccd.service.casedeletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
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

    public void deleteDocuments(long caseReference) {
        try {
            List<String> documents = pcsCaseService.getDocumentUrls(caseReference);
            if (!CollectionUtils.isEmpty(documents)) {
                deleteDocumentsFromCdam(documents, caseReference);
            }
        } catch (CaseNotFoundException e) {
            log.error("Case not found with reference: {} when deleting documents", caseReference, e);
        } catch (Exception e) {
            throw new DocumentDeletionException(caseReference);
        }
    }

    public void deleteDocumentsFromCdam(List<String> documentUrls, long caseReference) {
        if (!CollectionUtils.isEmpty(documentUrls)) {
            try {
                pcsCaseService.deleteDocumentsFromCdam(documentUrls, caseReference);
            } catch (Exception e) {
                throw new DocumentDeletionException(caseReference);
            }
        }
    }

    @Transactional
    public void deleteCaseData(long caseReference) {
        deletePcsCase(caseReference);
        deleteDraftData(caseReference);
        deleteCcdCase(caseReference);
    }

    @Transactional
    public void deleteCaseData(PcsCaseEntity pcsCaseEntity) {
        deletePcsCase(pcsCaseEntity);
        deleteDraftData(pcsCaseEntity.getCaseReference());
        deleteCcdCase(pcsCaseEntity.getCaseReference());
    }

    public void deletePcsCase(long caseReference) {
        try {
            pcsCaseService.deleteCase(caseReference);
        } catch (CaseNotFoundException e) {
            log.error("Case not found with reference: {} when deleting PcsCase", caseReference, e);
        } catch (Exception e) {
            log.error("Unexpected Error occurred while deleting PcsCase with reference: {}", caseReference, e);
            throw new PcsCaseDeletionException(caseReference);
        }
    }

    public void deletePcsCase(PcsCaseEntity pcsCaseEntity) {
        try {
            pcsCaseService.deleteCase(pcsCaseEntity);
        } catch (CaseNotFoundException e) {
            log.error("Case not found with reference: {} when deleting PcsCase", pcsCaseEntity.getCaseReference(), e);
        } catch (Exception e) {
            log.error("Unexpected Error occurred while deleting PcsCase with reference: {}",
                      pcsCaseEntity.getCaseReference(), e);
            throw new PcsCaseDeletionException(pcsCaseEntity.getCaseReference());
        }
    }

    public void deleteDraftData(long caseReference) {
        try {
            draftCaseDataService.deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
        } catch (Exception e) {
            log.error("Unexpected Error occurred while deleting DraftData with reference: {}", caseReference, e);
            throw new DraftDataDeletionException(caseReference);
        }
    }

    public void deleteCcdCase(long caseReference) {
        try {
            ccdCaseDataDeletionService.deleteCcdCaseData(caseReference);
        } catch (Exception e) {
            log.error("Unexpected Error occurred while deleting CcdCase with reference: {}", caseReference, e);
            throw new CcdCaseDataDeletionException(caseReference);
        }
    }
}
