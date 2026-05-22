package uk.gov.hmcts.reform.pcs.notify.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.notify.service.DefendantResponseNotificationService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantResponseEntityListenerTest {

    @Mock
    private DefendantResponseNotificationService defendantResponseNotificationService;

    @InjectMocks
    private DefendantResponseEntityListener underTest;

    @Test
    void shouldSetPreviousStatusOnPostLoad() {
        DefendantResponseEntity entity = new DefendantResponseEntity();
        entity.setStatus(DefendantResponseStatus.CREATED);

        underTest.onPostLoad(entity);

        assertEquals(DefendantResponseStatus.CREATED, entity.getPreviousStatus());
    }

    @Test
    void shouldHandleNotificationOnPostPersistWhenStatusIsSubmitted() {
        UUID defendantResponseId = UUID.randomUUID();
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.SUBMITTED);
        when(entity.getId()).thenReturn(defendantResponseId);

        underTest.onPostPersist(entity);

        verify(defendantResponseNotificationService).sendEmailNotificationForNoCounterClaim(defendantResponseId);
    }

    @Test
    void shouldNotHandleNotificationOnPostPersistWhenStatusIsNotSubmitted() {
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.CREATED);

        underTest.onPostPersist(entity);

        verify(defendantResponseNotificationService, never()).sendEmailNotificationForNoCounterClaim(null);
    }

    @Test
    void shouldDoNothingOnPostUpdateWhenStatusHasNotChanged() {
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.CREATED);
        when(entity.getPreviousStatus()).thenReturn(DefendantResponseStatus.CREATED);

        underTest.onPostUpdate(entity);

        verify(defendantResponseNotificationService, never()).sendEmailNotificationForNoCounterClaim(null);
    }

    @Test
    void shouldHandleNotificationOnPostUpdateWhenStatusChangesToSubmitted() {
        UUID defendantResponseId = UUID.randomUUID();
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.SUBMITTED);
        when(entity.getPreviousStatus()).thenReturn(DefendantResponseStatus.CREATED);
        when(entity.getId()).thenReturn(defendantResponseId);

        underTest.onPostUpdate(entity);

        verify(defendantResponseNotificationService).sendEmailNotificationForNoCounterClaim(defendantResponseId);
    }

    @Test
    void shouldNotHandleNotificationOnPostUpdateWhenStatusChangesToSomethingElse() {
        DefendantResponseEntity entity = mock(DefendantResponseEntity.class);
        when(entity.getStatus()).thenReturn(DefendantResponseStatus.CREATED);
        when(entity.getPreviousStatus()).thenReturn(null);

        underTest.onPostUpdate(entity);

        verify(defendantResponseNotificationService, never()).sendEmailNotificationForNoCounterClaim(null);
    }
}
