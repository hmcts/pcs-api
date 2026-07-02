package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.UUID;

/**
 * Sends the claim packs for one case, in a transaction so lazy case data can be read. Each recipient is
 * resolved and posted independently; a failing recipient is logged and skipped, never aborting the case.
 * Each document in the envelope is recorded as its own {@code DOCUMENT_SENT} row (SUCCESS or FAILURE).
 */
@Service
@Slf4j
public class ClaimPackSender {

    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_PARTY_ID = "partyId";
    private static final String MDC_LETTER_TYPE = "letterType";
    private static final String MDC_LETTER_ID = "letterId";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";
    private static final String MDC_DOCUMENT_ID = "documentId";
    private static final String MDC_DOCUMENT_TYPE = "documentType";

    private final PcsCaseRepository pcsCaseRepository;
    private final ClaimPackSelector claimPackSelector;
    private final RecipientAddressResolver recipientAddressResolver;
    private final AddressMapper addressMapper;
    private final BulkPrintService bulkPrintService;
    private final AccessCodeActivityLogService accessCodeActivityLogService;

    public ClaimPackSender(PcsCaseRepository pcsCaseRepository,
                           ClaimPackSelector claimPackSelector,
                           RecipientAddressResolver recipientAddressResolver,
                           AddressMapper addressMapper,
                           BulkPrintService bulkPrintService,
                           AccessCodeActivityLogService accessCodeActivityLogService) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.claimPackSelector = claimPackSelector;
        this.recipientAddressResolver = recipientAddressResolver;
        this.addressMapper = addressMapper;
        this.bulkPrintService = bulkPrintService;
        this.accessCodeActivityLogService = accessCodeActivityLogService;
    }

    @Transactional
    public void sendClaimPacks(UUID caseId) {
        pcsCaseRepository.findById(caseId).ifPresent(pcsCase ->
            claimPackSelector.findClaimPackCandidates(pcsCase)
                .forEach(candidate -> sendToRecipient(pcsCase, candidate)));
    }

    private void sendToRecipient(PcsCaseEntity pcsCase, ClaimPackCandidate candidate) {
        PartyEntity recipient = candidate.party();
        PartyRole role = candidate.recipientType();
        LetterType letterType = letterTypeFor(role);
        List<DocumentEntity> documents = candidate.documents();

        MDC.put(MDC_CASE_REFERENCE, String.valueOf(pcsCase.getCaseReference()));
        MDC.put(MDC_PARTY_ID, String.valueOf(recipient.getId()));
        MDC.put(MDC_LETTER_TYPE, letterType.name());
        try {
            String recipientName = recipientAddressResolver.resolveDisplayName(recipient);
            AddressUK address = resolveAddress(recipient, role, pcsCase.getPropertyAddress());
            UUID letterId = bulkPrintService.sendPack(
                pcsCase, recipient, letterType, recipientName, address, documents);
            MDC.put(MDC_LETTER_ID, String.valueOf(letterId));
            documents.forEach(document ->
                recordAndLogDocumentSent(pcsCase, recipient, document, letterType, letterId));
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

    private void recordAndLogDocumentSent(PcsCaseEntity pcsCase, PartyEntity recipient, DocumentEntity document,
                                          LetterType letterType, UUID letterId) {
        accessCodeActivityLogService.recordDocumentSent(pcsCase, recipient, document);
        MDC.put(MDC_DOCUMENT_ID, String.valueOf(document.getId()));
        MDC.put(MDC_DOCUMENT_TYPE, String.valueOf(document.getType()));
        log.info("Document sent - case: {}, party: {}, documentType: {}, documentId: {}, letterType: {}, letterId: {}",
            pcsCase.getCaseReference(), recipient.getId(), document.getType(), document.getId(),
            letterType.getCode(), letterId);
    }

    private AddressUK resolveAddress(PartyEntity recipient, PartyRole role, AddressEntity propertyAddress) {
        AddressEntity postalAddress = recipientAddressResolver.resolvePostalAddress(recipient, role, propertyAddress);
        return postalAddress == null ? null : addressMapper.toAddressUK(postalAddress);
    }

    private void recordFailure(PcsCaseEntity pcsCase, PartyEntity recipient, List<DocumentEntity> documents,
                               Exception cause, boolean terminal) {
        MDC.put(MDC_TERMINAL_FAILURE, String.valueOf(terminal));
        MDC.put(MDC_FAILURE_REASON, String.valueOf(cause.getMessage()));
        documents.forEach(document ->
            recordAndLogSendFailure(pcsCase, recipient, document, cause, terminal));
    }

    private void recordAndLogSendFailure(PcsCaseEntity pcsCase, PartyEntity recipient, DocumentEntity document,
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

    private LetterType letterTypeFor(PartyRole role) {
        return role == PartyRole.CLAIMANT ? LetterType.CLAIMANT_CLAIM_PACK : LetterType.DEFENDANT_CLAIM_PACK;
    }
}
