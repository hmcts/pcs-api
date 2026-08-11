package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CaseDeletionService;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;

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

    @Nested
    class PerformCaseDeletionTests {

        @Test
        void shouldDeleteCaseSuccessfully() {
            // Given & When
            long caseReference = 12345L;
            underTest.deleteCase(caseReference);

            // Then
            verify(ccdCaseDataDeletionService).deleteCcdCaseData(caseReference);
            verify(draftCaseDataService).deleteUnsubmittedCaseDataBySystemUser(caseReference, resumePossessionClaim);
            verify(pcsCaseService).deleteCase(caseReference);
        }
    }
}