package uk.gov.hmcts.reform.pcs.notify.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimStatus;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.notify.event.CounterClaimStatusUpdatedEvent;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CounterClaimEntityListenerTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private CounterClaimEntityListener underTest;

    @Test
    void shouldSetPreviousStatusOnPostLoad() {
        CounterClaimEntity entity = new CounterClaimEntity();
        entity.setStatus(CounterClaimStatus.PENDING_CASE_ISSUED);

        underTest.onPostLoad(entity);

        assertEquals(CounterClaimStatus.PENDING_CASE_ISSUED, entity.getPreviousStatus());
    }

    @Test
    void shouldPublishEventOnPostPersistWhenStatusIsPendingCaseIssued() {
        UUID counterClaimId = UUID.randomUUID();
        CounterClaimEntity entity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimStatus.PENDING_CASE_ISSUED)
            .build();

        underTest.onPostPersist(entity);

        ArgumentCaptor<CounterClaimStatusUpdatedEvent> eventCaptor =
            ArgumentCaptor.forClass(CounterClaimStatusUpdatedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        CounterClaimStatusUpdatedEvent event = eventCaptor.getValue();
        assertEquals(counterClaimId, event.getEntityId());
        assertEquals(CounterClaimStatus.PENDING_CASE_ISSUED, event.getNewStatus());
    }

    @Test
    void shouldNotPublishEventOnPostPersistWhenStatusIsNotPendingCaseIssued() {
        CounterClaimEntity entity = new CounterClaimEntity();
        entity.setStatus(CounterClaimStatus.CASE_ISSUED);

        underTest.onPostPersist(entity);

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldDoNothingOnPostUpdateWhenStatusHasNotChanged() {
        CounterClaimEntity entity = new CounterClaimEntity();
        entity.setStatus(CounterClaimStatus.PENDING_CASE_ISSUED);
        entity.setPreviousStatus(CounterClaimStatus.PENDING_CASE_ISSUED);

        underTest.onPostUpdate(entity);

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldPublishEventOnPostUpdateWhenStatusChanges() {
        UUID counterClaimId = UUID.randomUUID();
        CounterClaimEntity entity = CounterClaimEntity.builder()
            .id(counterClaimId)
            .status(CounterClaimStatus.PENDING_CASE_ISSUED)
            .previousStatus(CounterClaimStatus.CASE_ISSUED)
            .build();

        underTest.onPostUpdate(entity);

        ArgumentCaptor<CounterClaimStatusUpdatedEvent> eventCaptor =
            ArgumentCaptor.forClass(CounterClaimStatusUpdatedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        CounterClaimStatusUpdatedEvent event = eventCaptor.getValue();
        assertEquals(counterClaimId, event.getEntityId());
        assertEquals(CounterClaimStatus.CASE_ISSUED, event.getPreviousStatus());
        assertEquals(CounterClaimStatus.PENDING_CASE_ISSUED, event.getNewStatus());
    }
}
