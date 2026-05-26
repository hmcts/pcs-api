package uk.gov.hmcts.reform.pcs.notify.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefendantResponseEventListenerTest {

    @Mock
    private DefendantResponseNotificationService defendantResponseNotificationService;

    @InjectMocks
    private DefendantResponseEventListener underTest;

    @Test
    void shouldHandleNotificationWhenStatusIsSubmitted() {
        UUID defendantResponseId = UUID.randomUUID();
        DefendantResponseStatusUpdatedEvent event = new DefendantResponseStatusUpdatedEvent(
            defendantResponseId,
            DefendantResponseStatus.CREATED,
            DefendantResponseStatus.SUBMITTED
        );

        underTest.handle(event);

        verify(defendantResponseNotificationService).sendEmailNotificationForNoCounterClaim(defendantResponseId);
        verify(defendantResponseNotificationService).sendDefendantResponseReceived(defendantResponseId);
    }

    @Test
    void shouldNotHandleNotificationWhenStatusIsNotSubmitted() {
        UUID defendantResponseId = UUID.randomUUID();
        DefendantResponseStatusUpdatedEvent event = new DefendantResponseStatusUpdatedEvent(
            defendantResponseId,
            null,
            DefendantResponseStatus.CREATED
        );

        underTest.handle(event);

        verify(defendantResponseNotificationService, never())
            .sendEmailNotificationForNoCounterClaim(defendantResponseId);
        verify(defendantResponseNotificationService, never()).sendDefendantResponseReceived(defendantResponseId);
    }
}
