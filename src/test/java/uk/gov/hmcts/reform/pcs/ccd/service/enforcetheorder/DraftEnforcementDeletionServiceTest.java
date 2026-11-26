package uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftEnforcementDeletionServiceTest {

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;

    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    private DraftEnforcementDeletionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftEnforcementDeletionService(draftCaseDataRepository, enforcementOrderRepository);
    }

    @Test
    void shouldDeleteEnforcementDraftsOlderThanDiscardDays() {
        // Given
        long discardDays = 30L;
        Instant beforeInvocation = Instant.now();

        // When
        underTest.deleteExpiredDraftEnforcementApplications(discardDays);

        // Then
        Instant afterInvocation = Instant.now();
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(draftCaseDataRepository).deleteByEventIdAndCutoff(eq(EventId.enforceTheOrder),
                cutoffCaptor.capture()
        );

        Instant cutoff = cutoffCaptor.getValue();
        assertThat(cutoff)
                .isAfterOrEqualTo(beforeInvocation.minus(discardDays, ChronoUnit.DAYS))
                .isBeforeOrEqualTo(afterInvocation.minus(discardDays, ChronoUnit.DAYS));
    }

    @Test
    void shouldUseCurrentTimeAsCutoffWhenDiscardDaysIsZero() {
        // Given
        Instant beforeInvocation = Instant.now();

        // When
        underTest.deleteExpiredDraftEnforcementApplications(0L);

        // Then
        Instant afterInvocation = Instant.now();
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(draftCaseDataRepository).deleteByEventIdAndCutoff(eq(EventId.enforceTheOrder),
                cutoffCaptor.capture()
        );

        Instant cutoff = cutoffCaptor.getValue();
        assertThat(cutoff.isBefore(beforeInvocation)).isFalse();
        assertThat(cutoff.isAfter(afterInvocation)).isFalse();
    }

    @Test
    void shouldUseFutureCutoffWhenDiscardDaysIsNegative() {
        // Given
        long discardDays = -1L;
        Instant beforeInvocation = Instant.now();

        // When
        underTest.deleteExpiredDraftEnforcementApplications(discardDays);

        // Then
        Instant afterInvocation = Instant.now();
        ArgumentCaptor<Instant> draftCutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(draftCaseDataRepository).deleteByEventIdAndCutoff(
                eq(EventId.enforceTheOrder),
                draftCutoffCaptor.capture()
        );

        Instant cutoff = draftCutoffCaptor.getValue();
        assertThat(cutoff)
                .isAfterOrEqualTo(beforeInvocation.minus(discardDays, ChronoUnit.DAYS))
                .isBeforeOrEqualTo(afterInvocation.minus(discardDays, ChronoUnit.DAYS));
    }

    @Test
    void shouldDeletePendingEnforcementApplicationsOlderThanDiscardDays() {
        // Given
        long discardDays = 30L;
        Instant beforeInvocation = Instant.now();

        // When
        underTest.deleteExpiredDraftEnforcementApplications(discardDays);

        // Then
        Instant afterInvocation = Instant.now();
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(enforcementOrderRepository).deleteByLastModifiedAtBeforeAndStateIn(
                cutoffCaptor.capture(),
                eq(List.of(
                        EnforcementState.DRAFT_DISCARDED,
                        EnforcementState.AWAITING_SUBMISSION_TO_HMCTS,
                        EnforcementState.PENDING_CASE_ISSUED
                )));

        Instant cutoff = cutoffCaptor.getValue();
        assertThat(cutoff)
                .isAfterOrEqualTo(beforeInvocation.minus(discardDays, ChronoUnit.DAYS))
                .isBeforeOrEqualTo(afterInvocation.minus(discardDays, ChronoUnit.DAYS));
    }

    @Test
    void shouldDeleteDraftDataForApplicationsMarkedForDeletion() {
        List<Long> caseReferences = List.of(1001L, 1002L);
        when(enforcementOrderRepository.findCaseReferencesByState(EnforcementState.REQUESTED_FOR_DELETION))
            .thenReturn(caseReferences);

        underTest.deleteDraftEnforcementApplicationsMarkedForDeletion();

        var deletionOrder = inOrder(enforcementOrderRepository, draftCaseDataRepository);
        deletionOrder.verify(enforcementOrderRepository)
            .findCaseReferencesByState(EnforcementState.REQUESTED_FOR_DELETION);
        deletionOrder.verify(draftCaseDataRepository).deleteByEventIdAndCaseReferenceIn(
            EventId.enforceTheOrder,
            caseReferences
        );
        deletionOrder.verify(enforcementOrderRepository).updateStateByCaseReferenceInAndState(
            caseReferences,
            EnforcementState.REQUESTED_FOR_DELETION,
            EnforcementState.DRAFT_DISCARDED
        );
    }

    @Test
    void shouldNotDeleteDraftDataWhenNoApplicationsAreMarkedForDeletion() {
        when(enforcementOrderRepository.findCaseReferencesByState(EnforcementState.REQUESTED_FOR_DELETION))
            .thenReturn(List.of());

        underTest.deleteDraftEnforcementApplicationsMarkedForDeletion();

        verify(draftCaseDataRepository, never()).deleteByEventIdAndCaseReferenceIn(
            eq(EventId.enforceTheOrder),
            anyCollection()
        );
        verify(enforcementOrderRepository, never()).updateStateByCaseReferenceInAndState(
            anyCollection(),
            eq(EnforcementState.REQUESTED_FOR_DELETION),
            eq(EnforcementState.DRAFT_DISCARDED)
        );
    }
}