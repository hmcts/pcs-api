package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@ExtendWith(MockitoExtension.class)
class CaseDeletionServiceTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private CaseDeletionService underTest;

    @Test
    void shouldDeleteCaseSuccessfully() {
        // Given
        long caseReference = 12345L;

        // When
        underTest.deleteCase(caseReference);

        // Then
        verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
        verify(pcsCaseService).deleteCase(caseReference);
    }
}