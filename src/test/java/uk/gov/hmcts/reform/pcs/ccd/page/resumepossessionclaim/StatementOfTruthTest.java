package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatementOfTruthTest extends BasePageTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @BeforeEach
    void beforeEach() {
        setPageUnderTest(new StatementOfTruth(pcsCaseService));
    }

    @Test
    void shouldAllocateRegionId() {
        // Given
        PCSCase caseData = PCSCase.builder().build();

        // When
        callMidEventHandler(caseData);

        // Then
        verify(pcsCaseService).allocateRegionId(TEST_CASE_REFERENCE, caseData);
    }

}
