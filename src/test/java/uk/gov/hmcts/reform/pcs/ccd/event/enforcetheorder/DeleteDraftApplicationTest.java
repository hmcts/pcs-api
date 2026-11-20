package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@ExtendWith(MockitoExtension.class)
class DeleteDraftApplicationTest extends BaseEventTest {

    private static final String REFUSAL_EXPECTED_STRING = "This action is not available";

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private EnforcementFlagGuard enforcementFlagGuard;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new DeleteDraftApplication(draftCaseDataService, enforcementFlagGuard));
    }

    @Test
    void shouldBeConfiguredForCaseIssuedState() {
        assertConfiguredForStates(State.CASE_ISSUED);
    }

    @Test
    void shouldBeConfiguredAsShowForDraftResponseAndFeatureFlag() {
        assertConfiguredShowConditions(and("hasUnsubmittedEnforcementData=\"Yes\"", featureFlagsEnabled(ENFORCEMENT)));
    }

    @Test
    void shouldContainCorrectGrants() {
        assertGrants(UserRole.PCS_SOLICITOR, Permission.CRUD);
        assertGrants(UserRole.GA_CLAIMANT_SOLICITOR, Permission.CRUD);
        assertGrants(UserRole.CLAIMANT, Permission.CRUD);
    }

    @Test
    void shouldMakeDeleteDraftApplicationMandatory() {
        assertThat(getEventFieldIds()).contains("deleteDraftApplication");
    }

    @Test
    void shouldDeleteDraftApplicationWhenAnswerIsYes() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .deleteDraftApplication(YesOrNo.YES)
                    .build())
                .build();

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // Then
        verify(draftCaseDataService).deleteUnsubmittedCaseData(
                TEST_CASE_REFERENCE, enforceTheOrder);

        assertThat(submitResponse.getConfirmationBody())
                .contains("Draft application deleted");
    }

    @Test
    void shouldNotDeleteDraftApplicationWhenAnswerIsNo() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .enforcementOrder(EnforcementOrder.builder()
                    .deleteDraftApplication(YesOrNo.NO)
                    .build())
                .build();

        // When
        SubmitResponse<State> submitResponse = callSubmitHandler(caseData);

        // Then
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(
                TEST_CASE_REFERENCE, enforceTheOrder);

        assertThat(submitResponse.getConfirmationBody()).isNull();
    }

    @Test
    void givenEnforcementFlagOff_whenSubmitWithDeleteYes_thenReturnRefusalError() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .deleteDraftApplication(YesOrNo.YES)
                                  .build())
            .build();

        when(enforcementFlagGuard.isOff()).thenReturn(true);
        when(enforcementFlagGuard.refused()).thenCallRealMethod();
        List<String> expectedErrors = List.of(REFUSAL_EXPECTED_STRING);

        SubmitResponse<State> actualResponse = callSubmitHandler(caseData);

        assertThat(actualResponse.getErrors()).isEqualTo(expectedErrors);
    }

    @Test
    void givenEnforcementFlagOff_whenSubmitWithDeleteYes_thenDoNotCallDraftCaseDataService() {

        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .deleteDraftApplication(YesOrNo.YES)
                                  .build())
            .build();


        when(enforcementFlagGuard.isOff()).thenReturn(true);
        when(enforcementFlagGuard.refused()).thenCallRealMethod();
        callSubmitHandler(caseData);


        verifyNoInteractions(draftCaseDataService);
    }
}
