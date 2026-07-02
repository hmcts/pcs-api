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
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.UUID;

/**
 * Sends the defence-phase packs for one case in a transaction. Each recipient (the responding defendant, the
 * claimant, or a co-defendant served the counter-claim) is posted one envelope of their unsent documents.
 * A defendant's address is assertion-first; the claimant's is their own address. Each document is recorded as
 * its own {@code DOCUMENT_SENT} row; a failing recipient is logged and skipped, never aborting the case.
 */
@Service
@Slf4j
public class DefencePackSender {

    private static final String MDC_CASE_REFERENCE = "caseReference";
    private static final String MDC_PARTY_ID = "partyId";
    private static final String MDC_LETTER_TYPE = "letterType";
    private static final String MDC_LETTER_ID = "letterId";
    private static final String MDC_TERMINAL_FAILURE = "terminalFailure";
    private static final String MDC_FAILURE_REASON = "failureReason";
    private static final String MDC_DOCUMENT_ID = "documentId";
    private static final String MDC_DOCUMENT_TYPE = "documentType";

    private final PcsCaseRepository pcsCaseRepository;
    private final DefencePackSelector defencePackSelector;
    private final DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    private final RecipientAddressResolver recipientAddressResolver;
    private final AddressMapper addressMapper;
    private final BulkPrintService bulkPrintService;
    private final AccessCodeActivityLogService accessCodeActivityLogService;

    public DefencePackSender(PcsCaseRepository pcsCaseRepository,
                             DefencePackSelector defencePackSelector,
                             DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver,
                             RecipientAddressResolver recipientAddressResolver,
                             AddressMapper addressMapper,
                             BulkPrintService bulkPrintService,
                             AccessCodeActivityLogService accessCodeActivityLogService) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.defencePackSelector = defencePackSelector;
        this.defenceCorrespondenceAddressResolver = defenceCorrespondenceAddressResolver;
        this.recipientAddressResolver = recipientAddressResolver;
        this.addressMapper = addressMapper;
        this.bulkPrintService = bulkPrintService;
        this.accessCodeActivityLogService = accessCodeActivityLogService;
    }

    @Transactional
    public void sendDefencePacks(UUID caseId) {
        pcsCaseRepository.findById(caseId).ifPresent(pcsCase ->
            defencePackSelector.findDefencePackCandidates(pcsCase)
                .forEach(candidate -> sendToRecipient(pcsCase, candidate)));
    }

    private void sendToRecipient(PcsCaseEntity pcsCase, DefencePackCandidate candidate) {
        PartyEntity recipient = candidate.recipient();
        PartyRole role = candidate.role();
        List<DocumentEntity> documents = candidate.documents();

        MDC.put(MDC_CASE_REFERENCE, String.valueOf(pcsCase.getCaseReference()));
        MDC.put(MDC_PARTY_ID, String.valueOf(recipient.getId()));
        MDC.put(MDC_LETTER_TYPE, LetterType.DEFENCE_PACK.name());
        try {
            String recipientName = recipientAddressResolver.resolveDisplayName(recipient);
            AddressUK address = resolveAddress(recipient, role, pcsCase.getPropertyAddress());
            UUID letterId = bulkPrintService.sendPack(
                pcsCase, recipient, LetterType.DEFENCE_PACK, recipientName, address, documents);
            MDC.put(MDC_LETTER_ID, String.valueOf(letterId));
            documents.forEach(document ->
                recordAndLogDocumentSent(pcsCase, recipient, document, LetterType.DEFENCE_PACK, letterId));
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
        if (role == PartyRole.DEFENDANT) {
            return defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(recipient, propertyAddress);
        }
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
}
