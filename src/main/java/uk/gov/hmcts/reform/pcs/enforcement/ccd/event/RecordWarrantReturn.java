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
import uk.gov.hmcts.reform.pcs.enforcement.notification.WarrantReturnNotificationService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementChange;
import uk.gov.hmcts.reform.pcs.enforcement.service.EnforcementSideEffects;
import uk.gov.hmcts.reform.pcs.enforcement.service.RecordedWarrantReturn;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.recordWarrantReturn;
import static uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.BailiffStaffRoles.BAILIFF_STAFF_ROLES;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

@Component
@AllArgsConstructor
public class RecordWarrantReturn implements CCDConfig<PCSCase, State, UserRole> {

    static final String SHOW_CONDITION = featureFlagsEnabled(ENFORCEMENT);

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

        EnforcementChange<Void> notified = EnforcementSideEffects.schedule(
            caseReference, "the claimant's warrant return email",
            () -> warrantReturnNotificationService.notifyClaimant(caseReference, returnName,
                                                                  details.getAdditionalDetails()));
        if (notified.refused()) {
            return SubmitResponse.<State>builder().errors(notified.errors()).build();
        }

        return SubmitResponse.<State>builder()
            .confirmationHeader(CONFIRMATION_HEADER.formatted(returnName.getLabel()))
            .confirmationBody(ConfirmationNumbers.caseAndWarrant(
                caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference),
                recorded.change().warrantNumber()))
            .build();
    }
}
