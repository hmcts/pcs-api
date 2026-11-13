package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseDataDeletionException;
import uk.gov.hmcts.reform.pcs.exception.DraftDataDeletionException;
import uk.gov.hmcts.reform.pcs.exception.PcsCaseDeletionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@ExtendWith(MockitoExtension.class)
class CaseDeletionServiceTest {

    @Mock
    private CcdCaseDataDeletionService ccdCaseDataDeletionService;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private CaseDeletionService underTest;

    private final long caseRef = 1234L;

    @Test
    void shouldDeleteCaseData() {
        // Given
        // When
        underTest.deleteCaseData(caseRef);

        // Then
        verify(pcsCaseService).deleteCase(caseRef);
        verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseRef, resumePossessionClaim);
        verify(ccdCaseDataDeletionService).deleteCcdCaseData(caseRef);
    }

    @Test
    void shouldDeletePcsCaseSuccessfully() {
        // Given & When
        underTest.deletePcsCase(caseRef);

        // Then
        verify(pcsCaseService).deleteCase(caseRef);
    }

    @Test
    void shouldHandleCaseNotFoundExceptionWhenDeletingPcsCase() {
        // Given
        doThrow(CaseNotFoundException.class)
                .when(pcsCaseService).deleteCase(caseRef);

        // When
        underTest.deleteCaseData(caseRef);

        // Then
        verify(pcsCaseService).deleteCase(caseRef);
        verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseRef, resumePossessionClaim);
        verify(ccdCaseDataDeletionService).deleteCcdCaseData(caseRef);
    }

    @Test
    void shouldHandleExceptionWhenDeletingPcsCaseData() {
        // Given
        doThrow(RuntimeException.class)
                .when(pcsCaseService).deleteCase(caseRef);

        // When & Then
        assertThrows(PcsCaseDeletionException.class, () -> underTest.deletePcsCase(caseRef));
        verify(pcsCaseService).deleteCase(caseRef);
    }

    @Test
    void shouldDeleteDraftDataSuccessfully() {
        // Given & When
        underTest.deleteDraftData(caseRef);

        // Then
        verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseRef, resumePossessionClaim);
    }

    @Test
    void shouldHandleExceptionWhenDeletingDraftData() {
        // Given
        doThrow(RuntimeException.class)
                .when(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseRef, resumePossessionClaim);

        // When & Then
        assertThrows(DraftDataDeletionException.class, () -> underTest.deleteDraftData(caseRef));
        verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseRef, resumePossessionClaim);
    }

    @Test
    void shouldDeleteCcdCaseSuccessfully() {
        // Given & When
        underTest.deleteCcdCase(caseRef);

        // Then
        verify(ccdCaseDataDeletionService).deleteCcdCaseData(caseRef);
    }

    @Test
    void shouldHandleExceptionWhenDeletingCcdCaseData() {
        // Given
        doThrow(RuntimeException.class)
                .when(ccdCaseDataDeletionService).deleteCcdCaseData(caseRef);

        // When & Then
        assertThrows(CcdCaseDataDeletionException.class, () -> underTest.deleteCcdCase(caseRef));
        verify(ccdCaseDataDeletionService).deleteCcdCaseData(caseRef);
    }
}
