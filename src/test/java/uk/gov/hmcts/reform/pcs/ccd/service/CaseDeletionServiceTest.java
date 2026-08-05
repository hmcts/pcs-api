package uk.gov.hmcts.reform.pcs.ccd.service;

import feign.FeignException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseNotFoundException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    private final long caseReference = 12345L;

    @Nested
    class PerformCaseDeletionTests {

        @Test
        void shouldDeleteCaseAndDocumentsSuccessfully() {
            // Given & When
            underTest.performCaseDeletionTasks(caseReference);

            // Then
            verify(ccdCaseDataDeletionService).markCaseForDeletion(caseReference);
            verify(ccdCaseDataDeletionService).confirmCaseDisposal(caseReference);
            verify(pcsCaseService).deleteDocuments(caseReference);
            verify(ccdCaseDataDeletionService).deleteCcdCaseData(caseReference);
            verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
            verify(pcsCaseService).deleteCase(caseReference);
        }

        @Test
        void shouldCallMarkCaseForDeletionBeforeConfirmCaseDisposal() {
            // Given & When
            underTest.performCaseDeletionTasks(caseReference);

            // Then
            InOrder inOrder = inOrder(ccdCaseDataDeletionService);
            inOrder.verify(ccdCaseDataDeletionService).markCaseForDeletion(caseReference);
            inOrder.verify(ccdCaseDataDeletionService).confirmCaseDisposal(caseReference);
        }

        @Test
        void shouldContinueWithDeletionWhenCcdCaseNotFoundExceptionThrownFromMarkCaseForDisposal() {
            // Given
            doThrow(new CcdCaseNotFoundException(caseReference))
                    .when(ccdCaseDataDeletionService).markCaseForDeletion(caseReference);

            // When
            underTest.performCaseDeletionTasks(caseReference);

            // Then
            verify(ccdCaseDataDeletionService).markCaseForDeletion(caseReference);
            verify(ccdCaseDataDeletionService, never()).confirmCaseDisposal(caseReference);
            verify(pcsCaseService).deleteCase(caseReference);
            verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
            verify(pcsCaseService).deleteDocuments(caseReference);
        }

        @Test
        void shouldAbortCaseDeletionIfFeignExceptionThrownFromMarkCaseForDisposal() {
            // Given
            FeignException feignException = mock(FeignException.class);
            doThrow(feignException)
                    .when(ccdCaseDataDeletionService).markCaseForDeletion(caseReference);
            // When
            try {
                underTest.performCaseDeletionTasks(caseReference);
            } catch (FeignException e) {
                // Expected exception
            }
            // Then
            verify(ccdCaseDataDeletionService).markCaseForDeletion(caseReference);
            verify(ccdCaseDataDeletionService, never()).confirmCaseDisposal(caseReference);
            verify(pcsCaseService, never()).deleteDocuments(caseReference);
            verify(ccdCaseDataDeletionService, never()).deleteCcdCaseData(caseReference);
            verify(draftCaseDataService, never()).deleteUnsubmittedCaseDataBySystemUser(caseReference,
                    resumePossessionClaim);
            verify(pcsCaseService, never()).deleteCase(caseReference);
        }
    }

    @Nested
    class CleanupCaseTests {

        @Test
        void shouldCleanupCaseAndDocumentsSuccessfully() {
            // Given & When
            underTest.cleanupDiscardedDraftCases(caseReference);

            // Then
            verify(pcsCaseService).deleteDocuments(caseReference);
            verify(ccdCaseDataDeletionService).deleteCcdCaseData(caseReference);
            verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
            verify(pcsCaseService).deleteCase(caseReference);
        }
    }
}