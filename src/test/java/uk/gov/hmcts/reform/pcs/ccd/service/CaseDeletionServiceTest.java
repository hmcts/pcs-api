package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@ExtendWith(MockitoExtension.class)
class CaseDeletionServiceTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private PcsCaseRepository pcsCaseRepository;

    @InjectMocks
    private CaseDeletionService underTest;

    @Test
    void shouldDeleteCaseSuccessfully() {
        // Given
        long caseReference = 12345L;

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        when(pcsCaseService.loadCase(caseReference)).thenReturn(pcsCaseEntity); // Mock the case loading

        // When
        underTest.deleteCase(caseReference);

        // Then
        verify(draftCaseDataService).deleteUnsubmittedCaseData(caseReference, resumePossessionClaim);
        verify(pcsCaseService).loadCase(caseReference);
        verify(pcsCaseRepository).delete(pcsCaseEntity);
    }
}