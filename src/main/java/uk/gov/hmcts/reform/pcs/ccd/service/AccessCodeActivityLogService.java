package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimActivityLogEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimActivityLogRepository;

/**
 * Records rows to {@code claim_activity_log}: generation outcomes ({@code DOCUMENTS_CREATED}) and, for bulk
 * print, one {@code DOCUMENT_SENT} row per (recipient, document).
 */
@Service
@AllArgsConstructor
@Slf4j
public class AccessCodeActivityLogService {

    private final ClaimActivityLogRepository claimActivityLogRepository;

    public void logSuccess(PcsCaseEntity pcsCase, PartyEntity party, ClaimActivityType activityType) {
        save(pcsCase, party, null, activityType, ClaimActivityStatus.SUCCESS);
    }

    /**
     * Uses REQUIRES_NEW so the failure row survives a rollback in the caller's transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(PcsCaseEntity pcsCase, PartyEntity party, ClaimActivityType activityType) {
        save(pcsCase, party, null, activityType, ClaimActivityStatus.FAILURE);
    }

    /**
     * A document was posted to a recipient. The (recipient, document) SUCCESS row is the bulk-print dedup key,
     * so it commits in its own transaction: the post is irreversible, and the send runs outside any caller
     * transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDocumentSent(PcsCaseEntity pcsCase, PartyEntity recipient, DocumentEntity document) {
        save(pcsCase, recipient, document, ClaimActivityType.DOCUMENT_SENT, ClaimActivityStatus.SUCCESS);
    }

    /**
     * A document send to a recipient failed; REQUIRES_NEW so the row survives the caller's rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDocumentSendFailure(PcsCaseEntity pcsCase, PartyEntity recipient, DocumentEntity document) {
        save(pcsCase, recipient, document, ClaimActivityType.DOCUMENT_SENT, ClaimActivityStatus.FAILURE);
    }

    private void save(PcsCaseEntity pcsCase,
                      PartyEntity party,
                      DocumentEntity document,
                      ClaimActivityType activityType,
                      ClaimActivityStatus status) {

        claimActivityLogRepository.save(
            ClaimActivityLogEntity.builder()
                .pcsCase(pcsCase)
                .party(party)
                .document(document)
                .activityType(activityType)
                .status(status)
                .build()
        );

        log.debug("Recorded claim activity {} with status {} for case {}{}",
                  activityType,
                  status,
                  pcsCase.getId(),
                  party != null ? " and party " + party.getId() : "");
    }
}
