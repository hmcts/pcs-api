package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.PackDocumentRef;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Runs one recipient's send and records the outcome, shared by both pack senders. Sets the MDC context,
 * invokes the send, and writes one pack-grained {@code PACK_SENT}/{@code PACK_FAILED} row per dispatch —
 * SUCCESS with the documents it carried (the bulk-print dedup source), or FAILURE (terminal for a missing
 * address, retryable otherwise, so it self-heals on the next sweep). Per-document detail goes to the app
 * log / App Insights only.
 */
@Service
@Slf4j
public class PackSendRecorder {

    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_PARTY_ID = "partyId";
    private static final String MDC_LETTER_TYPE = "letterType";
    private static final String MDC_LETTER_ID = "letterId";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";

    private final AccessCodeActivityLogService accessCodeActivityLogService;

    public PackSendRecorder(AccessCodeActivityLogService accessCodeActivityLogService) {
        this.accessCodeActivityLogService = accessCodeActivityLogService;
    }

    public void sendAndRecord(PcsCaseEntity pcsCase, PartyEntity recipient, LetterType letterType,
                              List<DocumentEntity> documents, Supplier<UUID> sendAction) {
        MDC.put(MDC_CASE_REFERENCE, String.valueOf(pcsCase.getCaseReference()));
        MDC.put(MDC_PARTY_ID, String.valueOf(recipient.getId()));
        MDC.put(MDC_LETTER_TYPE, letterType.name());
        try {
            UUID letterId = sendAction.get();
            MDC.put(MDC_LETTER_ID, String.valueOf(letterId));
            accessCodeActivityLogService.recordPackSent(pcsCase, recipient,
                PackDetails.sent(letterType, packDocumentRefs(pcsCase, recipient, documents), letterId));
            log.info("Pack sent - case: {}, party: {}, letterType: {}, letterId: {}, documents: {}",
                pcsCase.getCaseReference(), recipient.getId(), letterType.getCode(), letterId,
                documentSummary(documents));
            documents.forEach(document -> logDocumentSent(pcsCase, recipient, document, letterType, letterId));
        } catch (MissingPostalAddressException e) {
            recordFailure(pcsCase, recipient, letterType, documents, e, true);
        } catch (Exception e) {
            recordFailure(pcsCase, recipient, letterType, documents, e, false);
        } finally {
            MDC.remove(MDC_CASE_REFERENCE);
            MDC.remove(MDC_PARTY_ID);
            MDC.remove(MDC_LETTER_TYPE);
            MDC.remove(MDC_LETTER_ID);
            MDC.remove(MDC_TERMINAL_FAILURE);
            MDC.remove(MDC_FAILURE_REASON);
        }
    }

    private void recordFailure(PcsCaseEntity pcsCase, PartyEntity recipient, LetterType letterType,
                               List<DocumentEntity> documents, Exception cause, boolean terminal) {
        MDC.put(MDC_TERMINAL_FAILURE, String.valueOf(terminal));
        MDC.put(MDC_FAILURE_REASON, String.valueOf(cause.getMessage()));
        accessCodeActivityLogService.recordPackFailed(pcsCase, recipient,
            PackDetails.failed(letterType, packDocumentRefs(pcsCase, recipient, documents), cause));
        if (terminal) {
            log.error("Pack send failed (terminal) - case: {}, party: {}, letterType: {}, documents: {}: {}",
                pcsCase.getCaseReference(), recipient.getId(), letterType.getCode(),
                documentSummary(documents), cause.getMessage(), cause);
        } else {
            log.warn("Pack send failed (will retry) - case: {}, party: {}, letterType: {}, documents: {}: {}",
                pcsCase.getCaseReference(), recipient.getId(), letterType.getCode(),
                documentSummary(documents), cause.getMessage());
        }
    }

    private void logDocumentSent(PcsCaseEntity pcsCase, PartyEntity recipient, DocumentEntity document,
                                 LetterType letterType, UUID letterId) {
        log.info("Document sent - case: {}, party: {}, documentType: {}, documentId: {}, letterType: {}, letterId: {}",
            pcsCase.getCaseReference(), recipient.getId(), document.getType(), document.getId(),
            letterType.getCode(), letterId);
    }

    private List<PackDocumentRef> packDocumentRefs(PcsCaseEntity pcsCase, PartyEntity recipient,
                                                   List<DocumentEntity> documents) {
        return documents.stream()
            .map(document -> {
                PartyEntity owner = owningParty(document);
                Integer defendantNumber = owner == null ? null : resolveDefendantNumber(pcsCase, owner.getId());
                boolean self = owner != null && owner.getId().equals(recipient.getId());
                return new PackDocumentRef(document.getId(), document.getType(), defendantNumber, self);
            })
            .toList();
    }

    // The party a document belongs to: on the document directly (access code, counter-claim) or via the
    // defence response (the defence form carries no direct party_id). Null for the case-level claim form.
    private PartyEntity owningParty(DocumentEntity document) {
        if (document.getParty() != null) {
            return document.getParty();
        }
        if (document.getDefendantResponse() != null) {
            return document.getDefendantResponse().getParty();
        }
        return null;
    }

    // The defendant's 1-based number from claim_party.rank (same source as the "Defence - Defendant N" name).
    private Integer resolveDefendantNumber(PcsCaseEntity pcsCase, UUID partyId) {
        if (pcsCase.getClaims().isEmpty()) {
            return null;
        }
        return pcsCase.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.DEFENDANT)
            .filter(claimParty -> claimParty.getParty().getId().equals(partyId))
            .map(ClaimPartyEntity::getRank)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private String documentSummary(List<DocumentEntity> documents) {
        return documents.stream()
            .map(document -> String.valueOf(document.getType()))
            .reduce((a, b) -> a + ", " + b)
            .orElse("none");
    }
}
