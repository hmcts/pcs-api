package uk.gov.hmcts.reform.pcs.notify.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;

@Service
@RequiredArgsConstructor
public class DefendantResponseEventListener {

    private final DefendantResponseNotificationService defendantResponseNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(DefendantResponseStatusUpdatedEvent event) {
        if (event.getNewStatus() == DefendantResponseStatus.SUBMITTED) {
            defendantResponseNotificationService.sendEmailNotificationForNoCounterClaim(event.getEntityId());
        }
    }
}
