package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.event.ClaimIssuePayment;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;

@AllArgsConstructor
@Component
@Slf4j
public class MakeAClaimPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final ClaimIssuePayment claimIssuePayment;
    private final PartyService partyService;
    private final ObjectMapper objectMapper;

    @Override
    public SystemEventResult handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity,
                                    String currentState) {
        FeesAndPayTaskData feesAndPayTaskData = toFeesAndPayTaskData(feePaymentEntity);
        PartyEntity claimParty = getResponsibleParty(feesAndPayTaskData);
        feePaymentEntity.setParty(claimParty);
        if (PaymentStatus.PAID == feePaymentEntity.getPaymentStatus()) {
            return claimIssuePayment.issue(feesAndPayTaskData.getCaseReference(), currentState);
        } else {
            log.warn("The payment was not successful [{}] for case: {}", feePaymentEntity.getPaymentStatus(),
                     feesAndPayTaskData.getCaseReference());
        }
        return null;
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
