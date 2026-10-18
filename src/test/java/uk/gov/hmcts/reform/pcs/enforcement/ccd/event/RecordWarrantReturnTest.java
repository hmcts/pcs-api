package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.BaseEventTest;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.PlacedFields;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn.EvictionDetailsMarkup;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn.RecordWarrantReturnConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn.RecordWarrantReturnPage;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.InterimWarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnType;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.notification.WarrantReturnNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.RecordedWarrantReturn;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;

@ExtendWith(MockitoExtension.class)
class RecordWarrantReturnTest extends BaseEventTest {

    @Mock
    private RecordWarrantReturnConfigurer recordWarrantReturnConfigurer;
    @Mock
    private WarrantReturnService warrantReturnService;
    @Mock
    private WarrantReturnNotificationService warrantReturnNotificationService;
    @Mock
    private EvictionDetailsMarkup evictionDetailsMarkup;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new RecordWarrantReturn(recordWarrantReturnConfigurer, warrantReturnService,
                                                  warrantReturnNotificationService, evictionDetailsMarkup,
                                                  new CaseReferenceFormatter()));
    }

    @AfterEach
    void clearSynchronisation() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldConfigureThePages() {
        verify(recordWarrantReturnConfigurer).configurePages(any());
    }

    @Test
    void shouldBeGatedOnTheEnforcementFeatureFlag() {
        assertConfiguredShowConditions("featureFlags.enforcementEnabled=\"YES\"");
    }

    @Test
    void shouldBeNamedAndEndWithCheckYourAnswers() {
        assertThat(configuredEvent.getName()).isEqualTo("Record warrant return");
        assertThat(configuredEvent.isShowSummary()).isTrue();
    }

    @Test
    void shouldBeAvailableInEveryStateOnceTheCaseIsIssued() {
        assertConfiguredForStates(AddBailiffNote.STATES);
    }

    @Test
    void shouldRetainTheEvictionDetailsWithoutPuttingTheMarkupOnCheckYourAnswers() {
        setEventUnderTest(new RecordWarrantReturn(
            new RecordWarrantReturnConfigurer(new RecordWarrantReturnPage(warrantReturnService)),
            warrantReturnService, warrantReturnNotificationService, evictionDetailsMarkup,
            new CaseReferenceFormatter()));

        Field<?, ?, ?, ?> markup = PlacedFields.find(configuredEvent.getFields(), "warrantReturnEvictionDetailsMarkup");

        assertThat(markup.isRetainHiddenValue()).isTrue();
        assertThat(markup.isShowSummary()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "ALLOCATED_BAILIFF", "BAILIFF", "BAILIFF_ADMIN", "BAILIFF_MANAGER", "HEARING_CENTRE_ADMIN",
        "HEARING_CENTRE_TEAM_LEADER"})
    void shouldGrantCrudToTheAllocatedBailiffAndBailiffStaff(UserRole userRole) {
        assertGrants(userRole, Permission.CRUD);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {
        "PCS_SOLICITOR", "GA_CLAIMANT_SOLICITOR", "CLAIMANT", "CLAIMANT_SOLICITOR", "DEFENDANT_SOLICITOR",
        "GA_DEFENDANT_SOLICITOR", "DEFENDANT", "CITIZEN", "JUDGE", "FEE_PAID_JUDGE", "CIRCUIT_JUDGE",
        "LEADERSHIP_JUDGE", "CTSC_ADMIN", "CTSC_TEAM_LEADER"})
    void shouldNotLetOtherUsersRecordAReturn(UserRole userRole) {
        assertThat(configuredEvent.getGrants().get(userRole)).doesNotContain(Permission.C, Permission.U);
    }

    @Test
    void shouldGrantHistoryOnlyAccessToJudicialHistoryRoles() {
        for (UserRole role : JUDICIAL_HISTORY_ROLES) {
            assertThat(configuredEvent.getGrants().get(role)).as("Grants for role %s", role)
                .containsExactly(Permission.R);
        }
    }

    // "Record warrant return needs a live warrant".
    @Test
    void shouldRefuseToStartWithoutAWarrant() {
        when(warrantReturnService.findWarrant(TEST_CASE_REFERENCE)).thenThrow(new NoEnforcementException());

        assertThatThrownBy(() -> callStartHandler(PCSCase.builder().build()))
            .isInstanceOf(NoEnforcementException.class)
            .hasMessage("There is no warrant on this case");
    }

    @Test
    void shouldRefuseToStartBeforeABailiffIsAllocated() {
        when(warrantReturnService.findWarrant(TEST_CASE_REFERENCE)).thenThrow(
            new EnforcementEventNotAvailableException(WarrantReturnService.NOT_ALLOCATED_MESSAGE));

        assertThatThrownBy(() -> callStartHandler(PCSCase.builder().build()))
            .isInstanceOf(EnforcementEventNotAvailableException.class);
    }

    @Test
    void shouldStartWithEmptyAnswersAndTheEvictionDetails() {
        PCSCase caseData = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                                  .warrantReturn(interim(InterimWarrantReturnName.CROWDS_OR_PROTESTS, "old"))
                                  .build())
            .build();
        when(evictionDetailsMarkup.build(caseData)).thenReturn("<table/>");

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getWarrantReturn())
            .isEqualTo(WarrantReturnDetails.builder().evictionDetailsMarkup("<table/>").build());
    }

    @Test
    void shouldStartWhenTheCaseViewHasNoEnforcementOrder() {
        PCSCase caseData = PCSCase.builder().build();
        when(evictionDetailsMarkup.build(caseData)).thenReturn("<table/>");

        PCSCase result = callStartHandler(caseData);

        assertThat(result.getEnforcementOrder().getWarrantReturn().getEvictionDetailsMarkup()).isEqualTo("<table/>");
    }

    // "Submitting records the return": "Outcome recorded as [return name]".
    @Test
    void shouldRecordTheReturnNotifyTheClaimantAndConfirm() {
        WarrantReturnDetails details = interim(InterimWarrantReturnName.OTHER_INTERIM_RETURN, "Court order shown");
        when(warrantReturnService.recordReturn(TEST_CASE_REFERENCE, details))
            .thenReturn(recorded(WarrantReturnName.OTHER_INTERIM_RETURN, "WP-00000042"));

        SubmitResponse<State> response = callSubmitHandler(caseWith(details));

        verify(warrantReturnNotificationService)
            .notifyClaimant(TEST_CASE_REFERENCE, WarrantReturnName.OTHER_INTERIM_RETURN, "Court order shown");
        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getConfirmationHeader()).isEqualTo("# Outcome recorded as Other interim return");
        assertThat(response.getConfirmationBody())
            .isEqualTo("Case number: 0000-0000-0000-1234\n\nWarrant number: WP-00000042");
    }

    @Test
    void shouldLeaveTheWarrantNumberOutOfTheConfirmationWhenTheWarrantHasNone() {
        WarrantReturnDetails details = interim(InterimWarrantReturnName.CROWDS_OR_PROTESTS, null);
        when(warrantReturnService.recordReturn(TEST_CASE_REFERENCE, details))
            .thenReturn(recorded(WarrantReturnName.CROWDS_OR_PROTESTS, null));

        SubmitResponse<State> response = callSubmitHandler(caseWith(details));

        assertThat(response.getConfirmationBody()).isEqualTo("Case number: 0000-0000-0000-1234");
    }

    /**
     * A failure to queue the email fails the submit, so the return is rolled back rather than recorded without
     * its email.
     */
    @Test
    void shouldFailTheSubmitWhenTheEmailCannotBeQueued() {
        WarrantReturnDetails details = interim(InterimWarrantReturnName.CROWDS_OR_PROTESTS, null);
        when(warrantReturnService.recordReturn(TEST_CASE_REFERENCE, details))
            .thenReturn(recorded(WarrantReturnName.CROWDS_OR_PROTESTS, "WP-00000042"));
        when(warrantReturnNotificationService.notifyClaimant(anyLong(), any(), any()))
            .thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> callSubmitHandler(caseWith(details)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldQueueTheClaimantEmailInsideTheSubmitTransaction() {
        WarrantReturnDetails details = interim(InterimWarrantReturnName.CROWDS_OR_PROTESTS, null);
        when(warrantReturnService.recordReturn(TEST_CASE_REFERENCE, details))
            .thenReturn(recorded(WarrantReturnName.CROWDS_OR_PROTESTS, "WP-00000042"));
        TransactionSynchronizationManager.initSynchronization();

        callSubmitHandler(caseWith(details));

        verify(warrantReturnNotificationService)
            .notifyClaimant(TEST_CASE_REFERENCE, WarrantReturnName.CROWDS_OR_PROTESTS, null);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    void shouldReturnTheValidationErrors() {
        WarrantReturnDetails details = interim(InterimWarrantReturnName.OTHER_INTERIM_RETURN, null);
        when(warrantReturnService.recordReturn(TEST_CASE_REFERENCE, details))
            .thenReturn(EnforcementChange.refused(List.of("Enter additional details")));

        SubmitResponse<State> response = callSubmitHandler(caseWith(details));

        assertThat(response.getErrors()).containsExactly("Enter additional details");
        assertThat(response.getConfirmationHeader()).isNull();
        verify(warrantReturnNotificationService, never()).notifyClaimant(anyLong(), any(), any());
    }

    @Test
    void shouldReturnAnErrorWhenTheWarrantHasGone() {
        when(warrantReturnService.recordReturn(TEST_CASE_REFERENCE, null))
            .thenReturn(EnforcementChange.refused(NoEnforcementException.MESSAGE));

        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly("There is no warrant on this case");
        verify(warrantReturnNotificationService, never()).notifyClaimant(anyLong(), any(), any());
    }

    private static EnforcementChange<RecordedWarrantReturn> recorded(WarrantReturnName returnName,
                                                                     String warrantNumber) {
        return EnforcementChange.made(new RecordedWarrantReturn(returnName, warrantNumber));
    }

    private static PCSCase caseWith(WarrantReturnDetails details) {
        return PCSCase.builder().enforcementOrder(EnforcementOrder.builder().warrantReturn(details).build()).build();
    }

    private static WarrantReturnDetails interim(InterimWarrantReturnName returnName, String additionalDetails) {
        return WarrantReturnDetails.builder()
            .type(WarrantReturnType.INTERIM)
            .interimReturnName(returnName)
            .additionalDetails(additionalDetails)
            .build();
    }
}
