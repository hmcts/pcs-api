package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCcdPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final CcdPaymentStateUpdateService ccdPaymentStateUpdateService;
    private final PartyService partyService;
    private final ObjectMapper objectMapper;

    protected abstract EventId successEventId();

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        FeesAndPayTaskData taskData = toFeesAndPayTaskData(feePaymentEntity);
        feePaymentEntity.setParty(getResponsibleParty(taskData));
        if (PaymentStatus.PAID == feePaymentEntity.getPaymentStatus()) {
            ccdPaymentStateUpdateService.submitPaymentSuccess(taskData.getCaseReference(), successEventId());
        } else {
            log.warn("The {} was not successful [{}] for case: {}",
                     successEventId(), feePaymentEntity.getPaymentStatus(), taskData.getCaseReference());
        }
    }

    private FeesAndPayTaskData toFeesAndPayTaskData(FeePaymentEntity feePaymentEntity) {
        String taskData = feePaymentEntity.getTaskData();
        try {
            log.info("Reading taskdata for {} to FeesAndPayTaskData: {}", feePaymentEntity.getId(), taskData);
            return objectMapper.readValue(taskData, FeesAndPayTaskData.class);
        } catch (IOException e) {
            throw new PaymentCallbackException("Unable to process: " + taskData, e);
        }
    }

    private PartyEntity getResponsibleParty(FeesAndPayTaskData feesAndPayTaskData) {
        return partyService.getPartyEntityByEntityId(
            feesAndPayTaskData.getResponsiblePartyId(),
            feesAndPayTaskData.getCaseReference()
        );
    }

}
