package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdPaymentStateUpdateService;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

@AllArgsConstructor
@Component
@Slf4j
public class MakeAClaimPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final CcdPaymentStateUpdateService ccdPaymentStateUpdateService;
    private final PartyService partyService;
    private final PcsCaseService pcsCaseService;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;
    private final ClaimRepository claimRepository;

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        FeesAndPayTaskData feesAndPayTaskData = toFeesAndPayTaskData(feePaymentEntity.getTaskData());
        PartyEntity claimParty = getResponsibleParty(feesAndPayTaskData);
        feePaymentEntity.setParty(claimParty);
        if (PaymentStatus.PAID == feePaymentEntity.getPaymentStatus()) {
            pcsCaseService.allocateCaseManagementLocation(feesAndPayTaskData.getCaseReference());
            ccdPaymentStateUpdateService.submitPaymentSuccess(feesAndPayTaskData.getCaseReference());
            issueClaim(feePaymentEntity);
        } else {
            log.warn("The payment was not successful [{}] for case: {}", feePaymentEntity.getPaymentStatus(),
                     feesAndPayTaskData.getCaseReference());
        }
    }

    private void issueClaim(FeePaymentEntity feePaymentEntity) {
        ClaimEntity claim = feePaymentEntity.getClaim();
        claim.setClaimIssuedDate(LocalDateTime.now(utcClock));
        claimRepository.save(claim);
    }

    private FeesAndPayTaskData toFeesAndPayTaskData(String feesAndPayTaskDataAsString) {
        try {
            return objectMapper.readValue(feesAndPayTaskDataAsString, FeesAndPayTaskData.class);
        } catch (IOException e) {
            throw new PaymentCallbackException("Unable to process: " + feesAndPayTaskDataAsString, e);
        }
    }

    private PartyEntity getResponsibleParty(FeesAndPayTaskData feesAndPayTaskData) {
        return partyService.getPartyEntityByEntityId(
            feesAndPayTaskData.getResponsiblePartyId(),
            feesAndPayTaskData.getCaseReference()
        );
    }

}
