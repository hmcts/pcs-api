package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class HearingNoticePackSender {

    private final PackRecipientResolver packRecipientResolver;
    private final BulkPrintService bulkPrintService;
    private final PackSendRecorder packSendRecorder;

    public void sendHearingNoticePacks(UUID caseId) {
        packRecipientResolver.resolveHearingNoticeRecipients(caseId).forEach(this::post);
    }

    private void post(ResolvedRecipient resolved) {
        packSendRecorder.sendAndRecord(
            resolved.pcsCase(), resolved.recipient(), resolved.letterType(), resolved.documents(),
            () -> bulkPrintService.sendPack(resolved.pcsCase(), resolved.recipient(), resolved.letterType(),
                                            resolved.recipientName(), resolved.address(), resolved.documents()));
    }
}
