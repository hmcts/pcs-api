package uk.gov.hmcts.reform.pcs.ccd.event.changecasestate;

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
import uk.gov.hmcts.reform.pcs.ccd.page.changecasestate.ChangeCaseStatePage;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.changeCaseState;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;

@Component
@AllArgsConstructor
public class ChangeCaseState implements CCDConfig<PCSCase, State, UserRole> {

    private final AddressFormatter addressFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(changeCaseState.name(), this::submit)
            .forStates(
                State.CASE_ISSUED,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME,
                State.CASE_PROGRESSION,
                State.ALL_FINAL_ORDERS_ISSUED,
                State.CASE_STAYED,
                State.BREATHING_SPACE
            )
            .name("Change case state")
            .grant(Permission.CRUD, CASEWORKER_ROLES)
            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
            .showSummary()
            .endButtonLabel("Submit");

        new PageBuilder(eventBuilder)
            .add(new ChangeCaseStatePage());
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        State targetState = pcsCase.getTargetState().toState();
        return SubmitResponse.<State>builder()
            .state(targetState)
            .confirmationBody(buildConfirmationMarkdown(pcsCase, eventPayload.caseReference()))
            .build();
    }

    private String buildConfirmationMarkdown(PCSCase pcsCase, long caseReference) {
        String address = addressFormatter.formatShortAddress(pcsCase.getPropertyAddress(), COMMA_DELIMITER);
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Case state changed</span><br>
            <span class="govuk-panel__body">Case number: %s</span><br>
            <span class="govuk-panel__body">Property address: %s</span>
            </div>

            <h3 class="govuk-heading-s">What happens next</h3>
            <p class="govuk-body govuk-!-margin-bottom-6">The case will progress according to the state it has been
            changed to.</p>
            """.formatted(caseReference, address);
    }
}
