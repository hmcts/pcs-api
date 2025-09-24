package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CitizenUpdateApplicationTest extends BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new CitizenUpdateApplication(pcsCaseService));
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
        long caseReference = 1234L;
        PCSCase caseData = mock(PCSCase.class);

        callSubmitHandler(caseData);

        verify(pcsCaseService).patchCase(caseReference, caseData);
    }

}
