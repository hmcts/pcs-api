package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.repository.CcdCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CcdCaseDataDeletionServiceTest {

    private static final long CASE_REF = 123456789L;

    @Mock
    private CcdCaseRepository ccdCaseRepository;

    @InjectMocks
    private CcdCaseDataDeletionService underTest;

    @Test
    void shouldInvokeDeleteCcdCaseData() {
        underTest.deleteCcdCaseData(CASE_REF);

        verify(ccdCaseRepository).deleteCcdCaseData(CASE_REF);
    }
}
