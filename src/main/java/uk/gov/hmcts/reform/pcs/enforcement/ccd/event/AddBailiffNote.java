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
import uk.gov.hmcts.reform.pcs.enforcement.ccd.page.bailiffnote.AddBailiffNoteConfigurer;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.service.BailiffNoteService;

import java.util.EnumSet;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.and;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.featureFlagsEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.addBailiffNote;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;
import static uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.BailiffStaffRoles.BAILIFF_STAFF_ROLES;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.ENFORCEMENT;

/**
 * Bailiff staff add a note to the claim's current enforcement (HDPI-7593). The note goes to the live
 * enforcement, or to the most recent one when none is live, so notes can still be added after closure.
 */
@Component
@AllArgsConstructor
public class AddBailiffNote implements CCDConfig<PCSCase, State, UserRole> {

    /**
     * Offered once the claim has an enforcement (EnforcementEventAvailabilityView). The start callback still
     * refuses otherwise.
     */
    static final String SHOW_CONDITION = and(featureFlagsEnabled(ENFORCEMENT), "canAddBailiffNote=\"Yes\"");

    static final int MAX_NOTE_LENGTH = RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;
    static final String CONFIRMATION_HEADER = "# Bailiff note added";
    static final String NOTE_REQUIRED_ERROR = "Enter a note";
    static final String NOTE_TOO_LONG_ERROR = "The note must be 6,800 characters or fewer";

    /**
     * Enforcement is held apart from the case state, so the event is available in every state a case can be
     * in once it has been issued. Before that there can be no enforcement.
     */
    public static final State[] STATES = EnumSet.complementOf(EnumSet.of(
        State.AWAITING_SUBMISSION_TO_HMCTS,
        State.AWAITING_RESUBMISSION_TO_HMCTS,
        State.PENDING_CASE_ISSUED
    )).toArray(State[]::new);

    private final AddBailiffNoteConfigurer addBailiffNoteConfigurer;
    private final BailiffNoteService bailiffNoteService;
    private final CaseReferenceFormatter caseReferenceFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(addBailiffNote.name(), this::submit, this::start)
                .forStates(STATES)
                .name("Add a bailiff note")
                .grant(Permission.CRUD, BAILIFF_STAFF_ROLES)
                .showCondition(SHOW_CONDITION)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .endButtonLabel("Continue");
        addBailiffNoteConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        if (!bailiffNoteService.hasEnforcement(eventPayload.caseReference())) {
            throw new NoEnforcementException();
        }
        PCSCase pcsCase = eventPayload.caseData();
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        EnforcementOrder enforcementOrder = pcsCase.getEnforcementOrder();
        enforcementOrder.setBailiffNoteText(null);
        // For the page's "claimant vs defendants" line, as on decideEnforcement.
        enforcementOrder.setFormattedCaseName(pcsCase.getCaseNameHmctsInternal());
        return pcsCase;
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        EnforcementOrder enforcementOrder = eventPayload.caseData().getEnforcementOrder();
        String note = enforcementOrder == null ? null : enforcementOrder.getBailiffNoteText();

        if (note == null || note.isBlank()) {
            return errorResponse(NOTE_REQUIRED_ERROR);
        }
        if (note.length() > MAX_NOTE_LENGTH) {
            return errorResponse(NOTE_TOO_LONG_ERROR);
        }
        String warrantNumber;
        try {
            warrantNumber = bailiffNoteService.addNote(caseReference, note);
        } catch (NoEnforcementException ex) {
            return errorResponse(ex.getMessage());
        }
        return SubmitResponse.<State>builder()
            .confirmationHeader(CONFIRMATION_HEADER)
            .confirmationBody(ConfirmationNumbers.caseAndWarrant(
                caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference), warrantNumber))
            .build();
    }

    private static SubmitResponse<State> errorResponse(String error) {
        return SubmitResponse.<State>builder().errors(List.of(error)).build();
    }
}
