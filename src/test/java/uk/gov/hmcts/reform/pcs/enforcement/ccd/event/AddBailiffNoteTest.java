package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.bailiffnote.AddBailiffNoteConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@ExtendWith(MockitoExtension.class)
class AddBailiffNoteTest extends BaseEventTest {

    @Mock
    private AddBailiffNoteConfigurer addBailiffNoteConfigurer;
    @Mock
    private BailiffNoteService bailiffNoteService;

    private final CaseReferenceFormatter caseReferenceFormatter = new CaseReferenceFormatter();

    @BeforeEach
    void setUp() {
        setEventUnderTest(new AddBailiffNote(addBailiffNoteConfigurer, bailiffNoteService, caseReferenceFormatter));
    }

    @Test
    void shouldConfigureThePages() {
        verify(addBailiffNoteConfigurer).configurePages(any());
    }

    @Test
    void shouldBeGatedOnTheEnforcementFeatureFlag() {
        assertConfiguredShowConditions("featureFlags.enforcementEnabled=\"YES\"");
    }

    @Test
    void shouldBeAvailableInEveryStateOnceTheCaseIsIssued() {
        assertConfiguredForStates(
            State.CASE_ISSUED,
            State.PENDING_ENFORCEMENT_ISSUED,
            State.JUDICIAL_REFERRAL,
            State.HEARING_READINESS,
            State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
            State.DECISION_OUTCOME,
            State.CASE_PROGRESSION,
            State.ALL_FINAL_ORDERS_ISSUED,
            State.CASE_STAYED,
            State.BREATHING_SPACE,
            State.CLOSED
        );
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "BAILIFF", "BAILIFF_ADMIN", "BAILIFF_MANAGER", "HEARING_CENTRE_ADMIN", "HEARING_CENTRE_TEAM_LEADER"})
    void shouldGrantCrudToBailiffStaff(UserRole userRole) {
        assertGrants(userRole, Permission.CRUD);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "PCS_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT", "CLAIMANT_SOLICITOR", "DEFENDANT_SOLICITOR",
        "GA_DEFENDANT_SOLICITOR", "DEFENDANT", "CITIZEN", "JUDGE", "FEE_PAID_JUDGE", "CIRCUIT_JUDGE",
        "LEADERSHIP_JUDGE"})
    void shouldNotLetLegalRepresentativesOrJudgesAddANote(UserRole userRole) {
        assertThat(configuredEvent.getGrants().get(userRole)).doesNotContain(Permission.C, Permission.U);
    }

    @Test
    void shouldGrantHistoryOnlyAccessToJudicialHistoryRoles() {
        for (UserRole role : JUDICIAL_HISTORY_ROLES) {
            assertThat(configuredEvent.getGrants().get(role)).as("Grants for role %s", role)
                .containsExactly(Permission.R);
        }
    }

    @Test
    void shouldSubmitStraightFromTheNotePage() {
        assertThat(configuredEvent.getEndButtonLabel()).isEqualTo("Continue");
        assertThat(configuredEvent.isShowSummary()).isFalse();
    }

    @Test
    void shouldRefuseToStartWhenTheClaimHasNoEnforcement() {
        when(bailiffNoteService.hasEnforcement(TEST_CASE_REFERENCE)).thenReturn(false);
        PCSCase caseData = PCSCase.builder().build();

        assertThatThrownBy(() -> callStartHandler(caseData))
            .isInstanceOf(NoEnforcementException.class)
            .hasMessage("There is no warrant on this case");
    }

    @Test
    void shouldStartWithAnEmptyNote() {
        when(bailiffNoteService.hasEnforcement(TEST_CASE_REFERENCE)).thenReturn(true);
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().bailiffNoteText("left over").build())
            .caseNameHmctsInternal("Treetops Housing vs John Smith")
            .build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getBailiffNoteText()).isNull();
        assertThat(result.getEnforcementOrder().getFormattedCaseName()).isEqualTo("Treetops Housing vs John Smith");
    }

    @Test
    void shouldStartWhenTheCaseViewHasNoEnforcementOrder() {
        when(bailiffNoteService.hasEnforcement(TEST_CASE_REFERENCE)).thenReturn(true);
        PCSCase caseData = PCSCase.builder().build();

        PCSCase result = callStartHandler(caseData);

        assertThat(result).isSameAs(caseData);
        assertThat(result.getEnforcementOrder().getBailiffNoteText()).isNull();
    }

    @Test
    void shouldSaveTheNoteAndConfirm() {
        String note = "Dog on site * (large)";
        when(bailiffNoteService.addNote(TEST_CASE_REFERENCE, note)).thenReturn("WP-00000001");

        SubmitResponse<State> response = callSubmitHandler(caseWithNote(note));

        verify(bailiffNoteService).addNote(TEST_CASE_REFERENCE, note);
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getConfirmationHeader()).isEqualTo("# Bailiff note added");
        assertThat(response.getConfirmationBody())
            .isEqualTo("Case number: 0000-0000-0000-1234\n\nWarrant number: WP-00000001");
    }

    @Test
    void shouldAcceptANoteOfTheMaximumLength() {
        String note = "a".repeat(6800);

        SubmitResponse<State> response = callSubmitHandler(caseWithNote(note));

        verify(bailiffNoteService).addNote(TEST_CASE_REFERENCE, note);
        assertThat(response.getErrors()).isNullOrEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n"})
    void shouldRejectAMissingNote(String note) {
        SubmitResponse<State> response = callSubmitHandler(caseWithNote(note));

        assertThat(response.getErrors()).containsExactly("Enter a note");
        verify(bailiffNoteService, never()).addNote(anyLong(), anyString());
    }

    @Test
    void shouldRejectANoteWhenTheCaseDataHasNoEnforcementOrder() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly("Enter a note");
    }

    @Test
    void shouldRejectANoteThatIsTooLong() {
        SubmitResponse<State> response = callSubmitHandler(caseWithNote("a".repeat(6801)));

        assertThat(response.getErrors()).containsExactly("The note must be 6,800 characters or fewer");
        verify(bailiffNoteService, never()).addNote(anyLong(), anyString());
    }

    @Test
    void shouldReturnAnErrorWhenTheEnforcementHasGone() {
        doThrow(new NoEnforcementException()).when(bailiffNoteService).addNote(TEST_CASE_REFERENCE, "note");

        SubmitResponse<State> response = callSubmitHandler(caseWithNote("note"));

        assertThat(response.getErrors()).containsExactly("There is no warrant on this case");
        assertThat(response.getConfirmationHeader()).isNull();
    }

    private static PCSCase caseWithNote(String note) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().bailiffNoteText(note).build())
            .build();
    }
}
