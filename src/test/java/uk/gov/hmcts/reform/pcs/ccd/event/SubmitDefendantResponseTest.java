package uk.gov.hmcts.reform.pcs.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantContactPreferencesService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SubmitDefendantResponseTest extends BaseEventTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;

    @Mock
    private DefendantContactPreferencesService defendantContactPreferencesService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new SubmitDefendantResponse(
            draftCaseDataService,
            defendantContactPreferencesService
        ));
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
        verifyNoInteractions(defendantContactPreferencesService);
    }

    @Test
    void shouldSaveContactPreferencesWhenSubmitDraftIsYes() {
        // Given
        DefendantResponse defendantResponse = DefendantResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .contactByEmail(YesOrNo.NO)
            .contactByText(YesOrNo.YES)
            .contactByPost(YesOrNo.NO)
            .phoneNumber("07123456789")
            .email("defendant@example.com")
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        // When
        callSubmitHandler(caseData);

        // Then
        verify(defendantContactPreferencesService).saveContactPreferences(defendantResponse);
        verifyNoInteractions(draftCaseDataService);
    }

    @Test
    void shouldNotCallServicesWhenDefendantResponseIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .defendantResponse(null)
            .submitDraftAnswers(YesOrNo.YES)
            .build();

        // When
        callSubmitHandler(caseData);

        // Then
        verifyNoInteractions(defendantContactPreferencesService);
        verifyNoInteractions(draftCaseDataService);
    }

    @Test
    void shouldNotCallServicesWhenSubmitDraftAnswersIsNull() {
        // Given
        DefendantResponse defendantResponse = DefendantResponse.builder()
            .contactByPhone(YesOrNo.YES)
            .build();

        PCSCase caseData = PCSCase.builder()
            .defendantResponse(defendantResponse)
            .submitDraftAnswers(null)
            .build();

        // When
        callSubmitHandler(caseData);

        // Then
        verifyNoInteractions(defendantContactPreferencesService);
        verifyNoInteractions(draftCaseDataService);
    }

}
