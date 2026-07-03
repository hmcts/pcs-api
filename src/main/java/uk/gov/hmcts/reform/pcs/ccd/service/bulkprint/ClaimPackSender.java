package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sends the claim packs for one case. Recipients are resolved up front (in a read-only transaction inside
 * {@link PackRecipientResolver}); the render/fetch/merge/post then runs here with no transaction open, so no
 * DB connection is held across the external calls. Each recipient is posted independently; {@link
 * PackSendRecorder} records the outcome and isolates a failure so one skip never aborts the case.
 */
@Service
public class ClaimPackSender {

    private final PackRecipientResolver packRecipientResolver;
    private final BulkPrintService bulkPrintService;
    private final PackSendRecorder packSendRecorder;

    public ClaimPackSender(PackRecipientResolver packRecipientResolver,
                           BulkPrintService bulkPrintService,
                           PackSendRecorder packSendRecorder) {
        this.packRecipientResolver = packRecipientResolver;
        this.bulkPrintService = bulkPrintService;
        this.packSendRecorder = packSendRecorder;
    }

    public void sendClaimPacks(UUID caseId) {
        packRecipientResolver.resolveClaimRecipients(caseId).forEach(this::post);
    }

    private void post(ResolvedRecipient resolved) {
        packSendRecorder.send(resolved.pcsCase(), resolved.recipient(), resolved.letterType(),
            resolved.documents(),
            () -> bulkPrintService.sendPack(resolved.pcsCase(), resolved.recipient(), resolved.letterType(),
                resolved.recipientName(), resolved.address(), resolved.documents()));
    }
}
