package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static org.mockito.Mockito.verify;

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
    void shouldPatchUnsubmittedEventData() {
        // Given Defendant response
        DefendantResponse defendantResponse = DefendantResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .submitDraftAnswers(YesOrNo.NO)
            .build();

        // When
        callSubmitHandler(caseData);

        //Then
        verify(draftCaseDataService).patchUnsubmittedEventData(
            TEST_CASE_REFERENCE,
            defendantResponse,
            EventId.submitDefendantResponse
        );

    }

}
