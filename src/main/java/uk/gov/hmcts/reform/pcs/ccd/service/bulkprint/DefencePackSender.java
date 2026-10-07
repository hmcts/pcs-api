package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sends the defence-phase packs for one case. Eligible recipients are selected by {@link DefencePackSelector}
 * (postal defendants once the recipient rule is enabled; otherwise the previous all-party fan-out). Recipients
 * are resolved in a read-only transaction inside {@link PackRecipientResolver}; render/fetch/merge/post then
 * runs with no transaction open. {@link PackSendRecorder} records each document and isolates failures so one
 * skip never aborts the case.
 */
@Service
public class DefencePackSender {

    private final PackRecipientResolver packRecipientResolver;
    private final BulkPrintService bulkPrintService;
    private final PackSendRecorder packSendRecorder;

    public DefencePackSender(PackRecipientResolver packRecipientResolver,
                             BulkPrintService bulkPrintService,
                             PackSendRecorder packSendRecorder) {
        this.packRecipientResolver = packRecipientResolver;
        this.bulkPrintService = bulkPrintService;
        this.packSendRecorder = packSendRecorder;
    }

    public void sendDefencePacks(UUID caseId) {
        packRecipientResolver.resolveDefenceRecipients(caseId).forEach(this::post);
    }

    private void post(ResolvedRecipient resolved) {
        packSendRecorder.sendAndRecord(resolved.pcsCase(), resolved.recipient(), resolved.letterType(),
            resolved.documents(),
            () -> bulkPrintService.sendPack(resolved.pcsCase(), resolved.recipient(), resolved.letterType(),
                resolved.recipientName(), resolved.address(), resolved.documents()));
    }
}
