package uk.gov.hmcts.reform.pcs.notify.listener;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.notify.event.CounterClaimStatusUpdatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class CounterClaimEntityListener {

    private final ApplicationEventPublisher applicationEventPublisher;

    @PostLoad
    public void onPostLoad(CounterClaimEntity entity) {
        entity.setPreviousStatus(entity.getStatus());
    }

    @PostPersist
    public void onPostPersist(CounterClaimEntity entity) {
        if (entity.getStatus() == CounterClaimStatus.PENDING_COUNTER_CLAIM_ISSUED) {
            applicationEventPublisher.publishEvent(
                new CounterClaimStatusUpdatedEvent(entity.getId(), entity.getPreviousStatus(), entity.getStatus()));
        }
    }

    @PostUpdate
    public void onPostUpdate(CounterClaimEntity entity) {
        if (entity.getStatus() == entity.getPreviousStatus()) {
            return;
        }

        applicationEventPublisher.publishEvent(
            new CounterClaimStatusUpdatedEvent(entity.getId(), entity.getPreviousStatus(), entity.getStatus()));
    }
}
