package uk.gov.hmcts.reform.pcs.notify.listener;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;

@Slf4j
@Component
public class DefendantResponseEntityListener {

    private final DefendantResponseNotificationService defendantResponseNotificationService;

    public DefendantResponseEntityListener(
        @Lazy DefendantResponseNotificationService defendantResponseNotificationService
    ) {
        this.defendantResponseNotificationService = defendantResponseNotificationService;
    }


    @PostLoad
    public void onPostLoad(DefendantResponseEntity entity) {
        entity.setPreviousStatus(entity.getStatus());
    }

    @PostPersist
    public void onPostPersist(DefendantResponseEntity entity) {
        if (entity.getStatus() == DefendantResponseStatus.SUBMITTED) {
            defendantResponseNotificationService.sendEmailNotificationForNoCounterClaim(entity.getId());
        }
    }

    @PostUpdate
    public void onPostUpdate(DefendantResponseEntity entity) {
        if (entity.getStatus() == entity.getPreviousStatus()) {
            return;
        }

        if (entity.getStatus() == DefendantResponseStatus.SUBMITTED) {
            defendantResponseNotificationService.sendEmailNotificationForNoCounterClaim(entity.getId());
        }
    }
}
