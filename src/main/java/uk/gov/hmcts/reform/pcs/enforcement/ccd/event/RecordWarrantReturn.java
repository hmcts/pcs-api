package uk.gov.hmcts.reform.pcs.enforcement.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.ConfirmationNumbers;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn.EvictionDetailsMarkup;
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn.RecordWarrantReturnConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.exception.EnforcementEventNotAvailableException;
import uk.gov.hmcts.reform.pcs.enforcement.notification.WarrantReturnNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.RecordedWarrantReturn;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.recordWarrantReturn;
import static uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.BailiffStaffRoles.BAILIFF_STAFF_ROLES;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

/**
 * The allocated bailiff, or other bailiff staff, record a return on the claim's warrant (HDPI-7347, HDPI-8423,
 * HDPI-8424). It is available for a warrant of possession or restitution once a bailiff is allocated, and on a
 * closed warrant to reopen it. The return is kept as the warrant's history, moves its status as the return
 * name requires, and emails the claimant.
 *
 * <p>The "Record warrant return" task is created when the eviction is scheduled, due on the eviction date
 * ({@link uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementWaTaskService}), and closed by XUI through
 * the task-completion DMN when this event is submitted from it. Recording a return also cancels the
 * warrant's other open tasks, and asks for a new bailiff allocation when the return reopens it.</p>
 */
@Component
@AllArgsConstructor
public class RecordWarrantReturn implements CCDConfig<PCSCase, State, UserRole> {

    /**
     * Hidden only for a warrant waiting for a bailiff (EnforcementEventAvailabilityView). With no warrant it is
     * offered, and the start callback shows "There is no warrant on this case" (HDPI-7347).
     */
    static final String SHOW_CONDITION = and(featureFlagsEnabled(ENFORCEMENT), "canRecordWarrantReturn=\"Yes\"");

    static final String EVENT_NAME = "Record warrant return";
    static final String CONFIRMATION_HEADER = "# Outcome recorded as %s";

    private final RecordWarrantReturnConfigurer recordWarrantReturnConfigurer;
    private final WarrantReturnService warrantReturnService;
    private final WarrantReturnNotificationService warrantReturnNotificationService;
    private final EvictionDetailsMarkup evictionDetailsMarkup;
    private final CaseReferenceFormatter caseReferenceFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(recordWarrantReturn.name(), this::submit, this::start)
                // Enforcement is held apart from the case state, as for "Add a bailiff note".
                .forStates(AddBailiffNote.STATES)
                .name(EVENT_NAME)
                .grant(Permission.CRUD, BAILIFF_STAFF_ROLES)
                .grant(Permission.CRUD, UserRole.ALLOCATED_BAILIFF)
                .showCondition(SHOW_CONDITION)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary();
        recordWarrantReturnConfigurer.configurePages(new PageBuilder(eventBuilder));
        SummaryFields.excludeNeverShownFields(eventBuilder);
    }

    /**
     * Refuses to start without a warrant that can take a return, and starts with empty answers.
     *
     * @throws NoEnforcementException if the claim has no warrant
     * @throws EnforcementEventNotAvailableException if the warrant has no bailiff allocated yet
     */
    PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        warrantReturnService.findWarrant(eventPayload.caseReference());
        PCSCase pcsCase = eventPayload.caseData();
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        pcsCase.getEnforcementOrder().setWarrantReturn(WarrantReturnDetails.builder()
            .evictionDetailsMarkup(evictionDetailsMarkup.build(pcsCase))
            .build());
        return pcsCase;
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        EnforcementOrder enforcementOrder = eventPayload.caseData().getEnforcementOrder();
        WarrantReturnDetails details = enforcementOrder == null ? null : enforcementOrder.getWarrantReturn();

        EnforcementChange<RecordedWarrantReturn> recorded =
            warrantReturnService.recordReturn(caseReference, details);
        if (recorded.refused()) {
            return SubmitResponse.<State>builder().errors(recorded.errors()).build();
        }
        WarrantReturnName returnName = recorded.change().returnName();

        // Queued in the submit transaction, so the email is committed or rolled back with the return.
        warrantReturnNotificationService.notifyClaimant(caseReference, returnName, details.getAdditionalDetails());

        return SubmitResponse.<State>builder()
            .confirmationHeader(CONFIRMATION_HEADER.formatted(returnName.getLabel()))
            .confirmationBody(ConfirmationNumbers.caseAndWarrant(
                caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference),
                recorded.change().warrantNumber()))
            .build();
    }
}
