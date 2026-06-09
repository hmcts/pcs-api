package uk.gov.hmcts.reform.pcs.feesandpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.CounterClaimRepository;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class CounterClaimPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final CounterClaimRepository counterClaimRepository;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;

    public CounterClaimPaymentCallbackHandler(CounterClaimRepository counterClaimRepository,
                                              ObjectMapper objectMapper,
                                              @Qualifier("utcClock") Clock utcClock) {
        this.counterClaimRepository = counterClaimRepository;
        this.objectMapper = objectMapper;
        this.utcClock = utcClock;
    }

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        FeesAndPayTaskData feesAndPayTaskData = toFeesAndPayTaskData(feePaymentEntity.getTaskData());
        UUID counterClaimId = feesAndPayTaskData.getRelatedEntityId();
        if (counterClaimId == null) {
            throw new PaymentCallbackException(
                "Counterclaim payment callback missing relatedEntityId in task data",
                null
            );
        }

        CounterClaimEntity counterClaimEntity = counterClaimRepository.findById(counterClaimId)
            .orElseThrow(() -> new IllegalArgumentException("Counterclaim not found: " + counterClaimId));

        if (PaymentStatus.PAID == feePaymentEntity.getPaymentStatus()) {
            if (counterClaimEntity.getStatus() != CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED) {
                log.warn(
                    "Ignoring paid counterclaim payment callback for counterClaimId {} in status {}",
                    counterClaimId,
                    counterClaimEntity.getStatus()
                );
                return;
            }

            counterClaimEntity.setStatus(CounterClaimStatus.COUNTER_CLAIM_ISSUED);
            counterClaimEntity.setClaimIssuedDate(LocalDateTime.now(utcClock));
            counterClaimRepository.save(counterClaimEntity);
            return;
        }

        log.warn(
            "Counterclaim payment unsuccessful [{}] for counterClaimId {}",
            feePaymentEntity.getPaymentStatus(),
            counterClaimId
        );
    }

    private FeesAndPayTaskData toFeesAndPayTaskData(String feesAndPayTaskDataAsString) {
        try {
            return objectMapper.readValue(feesAndPayTaskDataAsString, FeesAndPayTaskData.class);
        } catch (IOException e) {
            throw new PaymentCallbackException("Unable to process: " + feesAndPayTaskDataAsString, e);
        }
    }
}
