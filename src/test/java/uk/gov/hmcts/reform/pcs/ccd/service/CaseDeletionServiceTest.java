package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    void shouldDeleteDocumentsIfAvailable() {
        // Given
        List<DocumentEntity> documents = List.of(new DocumentEntity(), new DocumentEntity());
        when(pcsCaseService.getDocuments(caseRef)).thenReturn(documents);

        // When
        underTest.deleteDocuments(caseRef);

        // Then
        verify(pcsCaseService).deleteDocuments(documents, caseRef);
    }

    @Test
    void shouldNotDeleteDocumentsIfNone() {
        // Given
        List<DocumentEntity> documents = List.of();
        when(pcsCaseService.getDocuments(caseRef)).thenReturn(documents);

        // When
        underTest.deleteDocuments(caseRef);

        // Then
        verify(pcsCaseService).getDocuments(caseRef);
        verify(pcsCaseService, never()).deleteDocuments(documents, caseRef);
    }

    @Test
    void shouldDeleteCaseSuccessfully() {
        // Given & When
        underTest.deleteCase(caseRef);

        // Then
        verify(ccdCaseDataDeletionService).deleteCcdCaseData(caseRef);
        verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseRef, resumePossessionClaim);
        verify(pcsCaseService).deleteCase(caseRef);
    }
}