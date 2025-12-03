package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

@ExtendWith(MockitoExtension.class)
class SubmitDefendantResponseTest extends BaseEventTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private PcsCaseService pcsCaseService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new SubmitDefendantResponse(draftCaseDataService, pcsCaseService));
    }

    @Test
    void shouldUpdateCaseOnSubmit() {
    }

}
