package uk.gov.hmcts.reform.pcs.notify.listener;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.notify.event.DefendantResponseStatusUpdatedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefendantResponseEntityListener {

    private final ApplicationEventPublisher applicationEventPublisher;

    @PostLoad
    public void onPostLoad(DefendantResponseEntity entity) {
        entity.setPreviousStatus(entity.getStatus());
    }

    @PostPersist
    public void onPostPersist(DefendantResponseEntity entity) {
        if (entity.getStatus() == DefendantResponseStatus.SUBMITTED) {
            applicationEventPublisher.publishEvent(
                new DefendantResponseStatusUpdatedEvent(
                    entity.getId(), entity.getPreviousStatus(), entity.getStatus()));
        }
    }

    @PostUpdate
    public void onPostUpdate(DefendantResponseEntity entity) {
        if (entity.getStatus() == entity.getPreviousStatus()) {
            return;
        }

        if (entity.getStatus() == DefendantResponseStatus.SUBMITTED) {
            applicationEventPublisher.publishEvent(
                new DefendantResponseStatusUpdatedEvent(
                    entity.getId(), entity.getPreviousStatus(), entity.getStatus()));
        }
    }
}
