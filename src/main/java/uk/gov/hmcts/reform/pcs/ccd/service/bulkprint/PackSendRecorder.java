package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Runs one recipient's send and records the outcome per document, shared by both pack senders. Sets the MDC
 * context, invokes the send, and writes a {@code DOCUMENT_SENT} row per document — SUCCESS, or FAILURE
 * (terminal for a missing address, retryable otherwise, so it self-heals on the next sweep).
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
    private static final String MDC_DOCUMENT_ID = "documentId";
    private static final String MDC_DOCUMENT_TYPE = "documentType";

    private final AccessCodeActivityLogService accessCodeActivityLogService;

    public PackSendRecorder(AccessCodeActivityLogService accessCodeActivityLogService) {
        this.accessCodeActivityLogService = accessCodeActivityLogService;
    }

    public void send(PcsCaseEntity pcsCase, PartyEntity recipient, LetterType letterType,
                     List<DocumentEntity> documents, Supplier<UUID> sendAction) {
        MDC.put(MDC_CASE_REFERENCE, String.valueOf(pcsCase.getCaseReference()));
        MDC.put(MDC_PARTY_ID, String.valueOf(recipient.getId()));
        MDC.put(MDC_LETTER_TYPE, letterType.name());
        try {
            UUID letterId = sendAction.get();
            MDC.put(MDC_LETTER_ID, String.valueOf(letterId));
            documents.forEach(document -> recordDocumentSent(pcsCase, recipient, document, letterType, letterId));
        } catch (MissingPostalAddressException e) {
            recordFailure(pcsCase, recipient, documents, e, true);
        } catch (Exception e) {
            recordFailure(pcsCase, recipient, documents, e, false);
        } finally {
            MDC.remove(MDC_CASE_REFERENCE);
            MDC.remove(MDC_PARTY_ID);
            MDC.remove(MDC_LETTER_TYPE);
            MDC.remove(MDC_LETTER_ID);
            MDC.remove(MDC_DOCUMENT_ID);
            MDC.remove(MDC_DOCUMENT_TYPE);
            MDC.remove(MDC_TERMINAL_FAILURE);
            MDC.remove(MDC_FAILURE_REASON);
        }
    }

    private void recordFailure(PcsCaseEntity pcsCase, PartyEntity recipient, List<DocumentEntity> documents,
                               Exception cause, boolean terminal) {
        MDC.put(MDC_TERMINAL_FAILURE, String.valueOf(terminal));
        MDC.put(MDC_FAILURE_REASON, String.valueOf(cause.getMessage()));
        documents.forEach(document -> recordDocumentSendFailure(pcsCase, recipient, document, cause, terminal));
    }

    private void recordDocumentSent(PcsCaseEntity pcsCase, PartyEntity recipient, DocumentEntity document,
                                    LetterType letterType, UUID letterId) {
        accessCodeActivityLogService.recordDocumentSent(pcsCase, recipient, document);
        MDC.put(MDC_DOCUMENT_ID, String.valueOf(document.getId()));
        MDC.put(MDC_DOCUMENT_TYPE, String.valueOf(document.getType()));
        log.info("Document sent - case: {}, party: {}, documentType: {}, documentId: {}, letterType: {}, letterId: {}",
            pcsCase.getCaseReference(), recipient.getId(), document.getType(), document.getId(),
            letterType.getCode(), letterId);
    }

    private void recordDocumentSendFailure(PcsCaseEntity pcsCase, PartyEntity recipient, DocumentEntity document,
                                           Exception cause, boolean terminal) {
        accessCodeActivityLogService.recordDocumentSendFailure(pcsCase, recipient, document);
        MDC.put(MDC_DOCUMENT_ID, String.valueOf(document.getId()));
        MDC.put(MDC_DOCUMENT_TYPE, String.valueOf(document.getType()));
        if (terminal) {
            log.error("Document send failed (terminal) - case: {}, party: {}, documentType: {}, documentId: {}: {}",
                pcsCase.getCaseReference(), recipient.getId(), document.getType(), document.getId(),
                cause.getMessage(), cause);
        } else {
            log.warn("Document send failed (will retry) - case: {}, party: {}, documentType: {}, documentId: {}: {}",
                pcsCase.getCaseReference(), recipient.getId(), document.getType(), document.getId(),
                cause.getMessage());
        }
    }
}
