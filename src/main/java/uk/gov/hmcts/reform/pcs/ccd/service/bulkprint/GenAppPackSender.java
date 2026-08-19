package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GenAppPackSender {

    private final PackRecipientResolver packRecipientResolver;
    private final BulkPrintService bulkPrintService;
    private final PackSendRecorder packSendRecorder;

    public GenAppPackSender(PackRecipientResolver packRecipientResolver,
                            BulkPrintService bulkPrintService,
                            PackSendRecorder packSendRecorder) {
        this.packRecipientResolver = packRecipientResolver;
        this.bulkPrintService = bulkPrintService;
        this.packSendRecorder = packSendRecorder;
    }

    public void sendGenAppPacks(UUID caseId) {
        packRecipientResolver.resolveGenAppRecipients(caseId).forEach(this::post);
    }

    private void post(ResolvedRecipient resolved) {
        packSendRecorder.sendAndRecord(resolved.pcsCase(), resolved.recipient(), resolved.letterType(),
            resolved.documents(),
            () -> bulkPrintService.sendPack(resolved.pcsCase(), resolved.recipient(), resolved.letterType(),
                resolved.recipientName(), resolved.address(), resolved.documents()));
    }
}
