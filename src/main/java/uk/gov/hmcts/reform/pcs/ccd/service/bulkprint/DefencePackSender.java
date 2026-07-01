package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;

import java.util.UUID;

/**
 * Sends the defence packs for one case in a transaction. Address is assertion-first; the pack status recorded
 * is the candidate's target (partially-sent while a counter-claim is outstanding, sent when complete). A
 * failing defendant is logged and skipped, never aborting the case.
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

    private final PcsCaseRepository pcsCaseRepository;
    private final DefencePackCandidateService defencePackCandidateService;
    private final DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    private final RecipientAddressResolver recipientAddressResolver;
    private final BulkPrintService bulkPrintService;
    private final AccessCodeActivityLogService accessCodeActivityLogService;

    public DefencePackSender(PcsCaseRepository pcsCaseRepository,
                             DefencePackCandidateService defencePackCandidateService,
                             DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver,
                             RecipientAddressResolver recipientAddressResolver,
                             BulkPrintService bulkPrintService,
                             AccessCodeActivityLogService accessCodeActivityLogService) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.defencePackCandidateService = defencePackCandidateService;
        this.defenceCorrespondenceAddressResolver = defenceCorrespondenceAddressResolver;
        this.recipientAddressResolver = recipientAddressResolver;
        this.bulkPrintService = bulkPrintService;
        this.accessCodeActivityLogService = accessCodeActivityLogService;
    }

    @Transactional
    public void sendDefencePacks(UUID caseId) {
        pcsCaseRepository.findById(caseId).ifPresent(pcsCase ->
            defencePackCandidateService.findDefencePackCandidates(pcsCase)
                .forEach(candidate -> sendToDefendant(pcsCase, candidate)));
    }

    private void sendToDefendant(PcsCaseEntity pcsCase, DefencePackCandidate candidate) {
        PartyEntity defendant = candidate.defendant();
        ClaimActivityType packStatus = candidate.targetStatus();

        MDC.put(MDC_CASE_REFERENCE, String.valueOf(pcsCase.getCaseReference()));
        MDC.put(MDC_PARTY_ID, String.valueOf(defendant.getId()));
        MDC.put(MDC_LETTER_TYPE, LetterType.DEFENCE_PACK.name());
        try {
            String recipientName = recipientAddressResolver.resolveDisplayName(defendant);
            AddressUK address = defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(
                defendant, pcsCase.getPropertyAddress());
            UUID letterId = bulkPrintService.sendPack(
                pcsCase, defendant, LetterType.DEFENCE_PACK, recipientName, address, candidate.documents());
            accessCodeActivityLogService.logSuccessInNewTransaction(pcsCase, defendant, packStatus);
            MDC.put(MDC_LETTER_ID, String.valueOf(letterId));
            log.info("Defence pack sent - case: {}, party: {}, status: {}, letterId: {}",
                pcsCase.getCaseReference(), defendant.getId(), packStatus, letterId);
        } catch (MissingPostalAddressException e) {
            recordFailure(pcsCase, defendant, packStatus, e, true);
        } catch (Exception e) {
            recordFailure(pcsCase, defendant, packStatus, e, false);
        } finally {
            MDC.remove(MDC_CASE_REFERENCE);
            MDC.remove(MDC_PARTY_ID);
            MDC.remove(MDC_LETTER_TYPE);
            MDC.remove(MDC_LETTER_ID);
            MDC.remove(MDC_TERMINAL_FAILURE);
            MDC.remove(MDC_FAILURE_REASON);
        }
    }

    private void recordFailure(PcsCaseEntity pcsCase, PartyEntity defendant, ClaimActivityType packStatus,
                               Exception cause, boolean terminal) {
        MDC.put(MDC_TERMINAL_FAILURE, String.valueOf(terminal));
        MDC.put(MDC_FAILURE_REASON, String.valueOf(cause.getMessage()));
        log.error("Defence pack failed - case: {}, party: {}, terminal: {}: {}",
            pcsCase.getCaseReference(), defendant.getId(), terminal, cause.getMessage(), cause);
        accessCodeActivityLogService.logFailure(pcsCase, defendant, packStatus);
    }
}
