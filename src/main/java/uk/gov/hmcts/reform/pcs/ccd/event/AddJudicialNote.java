package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.addjudicialnote.AddJudicialNotePage;
import uk.gov.hmcts.reform.pcs.ccd.service.JudicialNoteService;
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialNoteRoles.JUDICIAL_NOTES_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.addJudicialNote;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASE_MANAGEMENT_R2;

@Component
@AllArgsConstructor
public class AddJudicialNote implements CCDConfig<PCSCase, State, UserRole> {

    private final AddJudicialNotePage addJudicialNotePage;
    private final JudicialNoteService judicialNoteService;
    private final ConfirmationPanelBuilder confirmationPanelBuilder;

    private static final String CONFIRMATION_BODY = """
            ---
            %s

            <h3 class="govuk-heading-s">What happens next</h3>
            <p class="govuk-body govuk-!-margin-bottom-6">The judicial note has been added. It can be viewed in the
            'Judicial notes' tab on the case summary page.</p>
            """;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(addJudicialNote.name(), this::submit)
                .forStates(EventStates.addJudicialNote())
                .name("Add a judicial note")
                .showCondition(ShowConditions.featureFlagsEnabled(CASE_MANAGEMENT_R2))
                .grant(Permission.CRUD, JUDICIAL_NOTES_ROLES)
                .showSummary()
                .endButtonLabel("Submit");

        new PageBuilder(eventBuilder)
            .add(addJudicialNotePage);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        judicialNoteService.addJudicialNote(caseReference, pcsCase);

        String confirmationPanel = confirmationPanelBuilder.buildConfirmationPanel(
            "Judicial note added",
            List.of("You do not need to do anything else")
        );

        return SubmitResponse.<State>builder()
            .confirmationBody(CONFIRMATION_BODY.formatted(confirmationPanel))
            .build();
    }
}
