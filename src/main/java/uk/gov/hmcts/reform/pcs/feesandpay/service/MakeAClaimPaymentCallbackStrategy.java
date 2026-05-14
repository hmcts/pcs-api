package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.service.CcdUpdateService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;

import static uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService.PARTY_NOT_FOUND;

@AllArgsConstructor
@Component
public class MakeAClaimPaymentCallbackStrategy implements PaymentCallbackStrategy {

    private final CcdUpdateService ccdUpdateService;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        FeesAndPayTaskData feesAndPayTaskData = toFeesAndPayTaskData(feePaymentEntity.getTaskData());
        ClaimPartyEntity claimPartyEntity = retrieveClaimPartyEntity(feePaymentEntity.getClaim(),
                                                                     feesAndPayTaskData.getResponsibleParty());
        feePaymentEntity.setParty(claimPartyEntity.getParty());
        ccdUpdateService.submitPaymentSuccess(feesAndPayTaskData.getCaseReference());
    }

    private FeesAndPayTaskData toFeesAndPayTaskData(String feesAndPayTaskDataAsString) {
        try {
            return objectMapper.readValue(feesAndPayTaskDataAsString, FeesAndPayTaskData.class);
        } catch (IOException e) {
            throw new PaymentCallbackException("Unable to process: " + feesAndPayTaskDataAsString, e);
        }
    }

    private ClaimPartyEntity retrieveClaimPartyEntity(ClaimEntity claimEntity, String responsibleParty) {
        return claimEntity.getClaimParties()
            .stream()
            .filter(party -> responsibleParty.equals(party.getParty().getOrgName()))
            .findFirst()
            .orElseThrow(() -> new PartyNotFoundException(PARTY_NOT_FOUND));
    }

}
