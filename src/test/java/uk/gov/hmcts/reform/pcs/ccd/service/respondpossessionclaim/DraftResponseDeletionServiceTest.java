package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.exception.DraftResponseDataDeletionException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftResponseDeletionServiceTest {

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;

    @Mock
    private DraftCaseDataEntity draftCaseDataEntity;

    private DraftResponseDeletionService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftResponseDeletionService(draftCaseDataRepository);
    }

    @Test
    void shouldFindExpiredDraftResponses() {
        // Given
        List<DraftCaseDataEntity> expected = List.of(draftCaseDataEntity);
        when(draftCaseDataRepository.findExpiredDraftResponses(
            eq(EventId.respondPossessionClaim),
            any(LocalDateTime.class),
            eq(PageRequest.of(0, 50))
        )).thenReturn(expected);

        // When
        List<DraftCaseDataEntity> result = underTest.findExpiredDraftResponses(30, 50);

        // Then
        assertThat(result).isSameAs(expected);
        verify(draftCaseDataRepository).findExpiredDraftResponses(
            eq(EventId.respondPossessionClaim),
            any(LocalDateTime.class),
            eq(PageRequest.of(0, 50))
        );
    }

    @Test
    void shouldDeleteDraftData() {
        // When
        underTest.deleteDraftData(draftCaseDataEntity);

        // Then
        verify(draftCaseDataRepository).delete(draftCaseDataEntity);
    }

    @Test
    void shouldWrapDeleteException() {
        // Given
        long caseReference = 1234L;
        UUID partyId = UUID.randomUUID();
        String organisationId = "org-1";
        when(draftCaseDataEntity.getCaseReference()).thenReturn(caseReference);
        when(draftCaseDataEntity.getEventId()).thenReturn(EventId.respondPossessionClaim);
        when(draftCaseDataEntity.getPartyId()).thenReturn(partyId);
        when(draftCaseDataEntity.getOrganisationId()).thenReturn(organisationId);
        doThrow(new RuntimeException("boom")).when(draftCaseDataRepository).delete(draftCaseDataEntity);

        // When / Then
        assertThatThrownBy(() -> underTest.deleteDraftData(draftCaseDataEntity))
            .isInstanceOf(DraftResponseDataDeletionException.class)
            .hasMessageContaining("Failed to delete draft response data for case reference: " + caseReference)
            .hasMessageContaining("respondPossessionClaim")
            .hasMessageContaining(partyId.toString())
            .hasMessageContaining(organisationId);
    }
}
