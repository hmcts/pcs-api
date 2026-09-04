package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.ccd.sdk.SystemEventResult;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseIssueService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.claimIssuePayment;

/**
 * Handles the claim fee's paid callback by recording a {@code claimIssuePayment} system event: the
 * fee update, the case issue work and the state transition to Case issued commit in one transaction
 * with the case snapshot and the history entry. The CCPay callback can be re-fired safely - the
 * idempotency key is derived from the service request reference, so a replay records nothing twice.
 */
@AllArgsConstructor
@Component
@Slf4j
public class MakeAClaimPaymentCallbackHandler implements PaymentCallbackStrategy {

    private static final String EVENT_NAME = "Payment Confirmation";

    private final SystemEventExecutor systemEventExecutor;
    private final CaseIssueService caseIssueService;
    private final PartyService partyService;
    private final FeePaymentRepository feePaymentRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean handlesOwnTransaction(PaymentStatusCallback paymentStatusCallback) {
        return isPaid(paymentStatusCallback);
    }

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        FeesAndPayTaskData feesAndPayTaskData = toFeesAndPayTaskData(feePaymentEntity);
        PartyEntity claimParty = getResponsibleParty(feesAndPayTaskData);
        long caseReference = feesAndPayTaskData.getCaseReference();

        if (!isPaid(paymentStatusCallback)) {
            feePaymentEntity.setParty(claimParty);
            log.warn("The payment was not successful [{}] for case: {}",
                     paymentStatusCallback.getServiceRequestStatus(), caseReference);
            return;
        }

        systemEventExecutor.execute(caseReference, idempotencyKey(feePaymentEntity), context -> {
            feePaymentEntity.setExternalReference(paymentStatusCallback.getPaymentReference());
            feePaymentEntity.setPaymentStatus(PaymentStatus.PAID);
            feePaymentEntity.setParty(claimParty);
            feePaymentRepository.save(feePaymentEntity);

            caseIssueService.issueCaseIfNotIssued(caseReference);

            return SystemEventResult.withStateTransition(claimIssuePayment.name(), EVENT_NAME, State.CASE_ISSUED);
        });
    }

    private boolean isPaid(PaymentStatusCallback paymentStatusCallback) {
        return PaymentStatus.PAID == PaymentStatus.fromValue(paymentStatusCallback.getServiceRequestStatus());
    }

    private UUID idempotencyKey(FeePaymentEntity feePaymentEntity) {
        String key = claimIssuePayment.name() + ":" + feePaymentEntity.getServiceRequestReference();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
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
