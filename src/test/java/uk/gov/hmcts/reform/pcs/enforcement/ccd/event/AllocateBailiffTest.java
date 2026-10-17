package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.allocatebailiff.AllocateBailiffConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.BailiffAllocationDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.StaffUser;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffAllocationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffDirectoryService;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffUser;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@ExtendWith(MockitoExtension.class)
class AllocateBailiffTest extends BaseEventTest {

    private static final String SAM_ID = "sam-id";
    private static final BailiffUser SAM = new BailiffUser(SAM_ID, "Sam Jones", "sam.jones@justice.gov.uk");

    @Mock
    private AllocateBailiffConfigurer allocateBailiffConfigurer;
    @Mock
    private BailiffAllocationService bailiffAllocationService;
    @Mock
    private BailiffDirectoryService bailiffDirectoryService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new AllocateBailiff(allocateBailiffConfigurer, bailiffAllocationService,
                                              bailiffDirectoryService, new CaseNameFormatter(),
                                              new CaseReferenceFormatter()));
    }

    @Test
    void shouldConfigureThePages() {
        verify(allocateBailiffConfigurer).configurePages(any());
    }

    @Test
    void shouldBeGatedOnTheEnforcementFeatureFlag() {
        assertConfiguredShowConditions("featureFlags.enforcementEnabled=\"YES\"");
    }

    @Test
    void shouldBeAvailableInEveryStateOnceTheCaseIsIssued() {
        assertConfiguredForStates(AddBailiffNote.STATES);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"BAILIFF", "BAILIFF_ADMIN", "BAILIFF_MANAGER"})
    void shouldGrantCrudToBailiffs(UserRole userRole) {
        assertGrants(userRole, Permission.CRUD);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"HEARING_CENTRE_ADMIN", "HEARING_CENTRE_TEAM_LEADER"})
    void shouldShowTheHistoryToHearingCentreStaffWithoutLettingThemAllocate(UserRole userRole) {
        assertGrants(userRole, Set.of(Permission.R));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "PCS_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT", "CLAIMANT_SOLICITOR", "DEFENDANT_SOLICITOR",
        "GA_DEFENDANT_SOLICITOR", "DEFENDANT", "CITIZEN"})
    void shouldGiveThePartiesNoAccess(UserRole userRole) {
        assertThat(configuredEvent.getGrants().get(userRole)).isEmpty();
    }

    @Test
    void shouldGrantHistoryOnlyAccessToJudicialHistoryRoles() {
        for (UserRole role : JUDICIAL_HISTORY_ROLES) {
            assertThat(configuredEvent.getGrants().get(role)).as("Grants for role %s", role)
                .containsExactly(Permission.R);
        }
    }

    @Test
    void shouldShowCheckYourAnswers() {
        assertThat(configuredEvent.isShowSummary()).isTrue();
    }

    @Test
    void shouldRefuseToStartWhenTheWarrantCannotBeAllocated() {
        when(bailiffAllocationService.getAllocatableEnforcement(TEST_CASE_REFERENCE))
            .thenThrow(new EnforcementEventNotAvailableException("not yet"));

        assertThatThrownBy(() -> callStartHandler(PCSCase.builder().build()))
            .isInstanceOf(EnforcementEventNotAvailableException.class)
            .hasMessage("not yet");
    }

    @Test
    void shouldStartWithTheClaimantAndDefendantNamesAndNoBailiffChosen() {
        PCSCase caseData = PCSCase.builder()
            .allClaimants(List.of(new ListValue<>("1", Party.builder().orgName("Treetops Housing").build())))
            .allDefendants(List.of(new ListValue<>("2", Party.builder()
                .nameKnown(VerticalYesNo.YES).firstName("John").lastName("Smith").build())))
            .enforcementOrder(EnforcementOrder.builder()
                                  .bailiffAllocationDetails(BailiffAllocationDetails.builder()
                                                                .allocatedBailiff(new StaffUser("old"))
                                                                .allocatedBailiffEmail("old@example.com")
                                                                .build())
                                  .build())
            .build();

        PCSCase started = callStartHandler(caseData);

        BailiffAllocationDetails details = started.getEnforcementOrder().getBailiffAllocationDetails();
        assertThat(details.getBailiffAllocationCaseName()).isEqualTo("Treetops Housing vs John Smith");
        assertThat(details.getAllocatedBailiff()).isNull();
        assertThat(details.getAllocatedBailiffEmail()).isNull();
    }

    @Test
    void shouldAllocateTheChosenBailiffAndConfirmIt() {
        when(bailiffDirectoryService.findBailiff(SAM_ID)).thenReturn(Optional.of(SAM));

        SubmitResponse<State> response = callSubmitHandler(caseWithBailiff(SAM_ID));

        verify(bailiffAllocationService).allocate(TEST_CASE_REFERENCE, SAM);
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getConfirmationHeader()).isEqualTo("# Case assigned to Sam Jones");
        assertThat(response.getConfirmationBody()).isEqualTo("Case number: 0000-0000-0000-1234");
    }

    @Test
    void shouldConfirmWithTheEmailWhenTheBailiffHasNoName() {
        when(bailiffDirectoryService.findBailiff(SAM_ID))
            .thenReturn(Optional.of(new BailiffUser(SAM_ID, "", "sam.jones@justice.gov.uk")));

        SubmitResponse<State> response = callSubmitHandler(caseWithBailiff(SAM_ID));

        assertThat(response.getConfirmationHeader()).isEqualTo("# Case assigned to sam.jones@justice.gov.uk");
    }

    @Test
    void shouldAskForABailiffWhenNoneIsChosen() {
        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly("Choose a bailiff");
        verifyNoInteractions(bailiffDirectoryService);
        verify(bailiffAllocationService, never()).allocate(anyLong(), any());
    }

    @Test
    void shouldRefuseAUserWhoIsNotABailiff() {
        when(bailiffDirectoryService.findBailiff(SAM_ID)).thenReturn(Optional.empty());

        SubmitResponse<State> response = callSubmitHandler(caseWithBailiff(SAM_ID));

        assertThat(response.getErrors()).containsExactly("Choose a user who has the bailiff role");
        verify(bailiffAllocationService, never()).allocate(anyLong(), any());
    }

    @Test
    void shouldShowTheErrorWhenTheWarrantCanNoLongerBeAllocated() {
        when(bailiffDirectoryService.findBailiff(SAM_ID)).thenReturn(Optional.of(SAM));
        doThrow(new NoEnforcementException()).when(bailiffAllocationService).allocate(TEST_CASE_REFERENCE, SAM);

        SubmitResponse<State> response = callSubmitHandler(caseWithBailiff(SAM_ID));

        assertThat(response.getErrors()).containsExactly(NoEnforcementException.MESSAGE);
    }

    private static PCSCase caseWithBailiff(String userId) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .bailiffAllocationDetails(BailiffAllocationDetails.builder()
                                                                .allocatedBailiff(new StaffUser(userId))
                                                                .build())
                                  .build())
            .build();
    }
}
