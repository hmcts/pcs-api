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
import uk.gov.hmcts.reform.pcs.ccd.domain.documentamend.DocumentAmendDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.documentamend.AmendDocumentDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.documentamend.SelectDocumentPage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendSelectionService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.amendDocuments;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;

@Component
@AllArgsConstructor
public class AmendDocuments implements CCDConfig<PCSCase, State, UserRole> {

    private final DocumentAmendSelectionService documentAmendSelectionService;
    private final DocumentAmendService documentAmendService;
    private final AddressFormatter addressFormatter;
    private final SelectDocumentPage selectDocumentPage;
    private final AmendDocumentDetailsPage amendDocumentDetailsPage;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(amendDocuments.name(), this::submit, this::start)
                .forStates(
                    State.CASE_ISSUED,
                    State.JUDICIAL_REFERRAL,
                    State.HEARING_READINESS,
                    State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                    State.DECISION_OUTCOME,
                    State.CASE_PROGRESSION,
                    State.ALL_FINAL_ORDERS_ISSUED,
                    State.CASE_STAYED,
                    State.BREATHING_SPACE,
                    State.CLOSED
                )
                .name("Manage documents: Amend")
                .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_2, CASEWORKER_EVENTS))
                .grant(Permission.CRU, UserRole.HEARING_CENTRE_TEAM_LEADER)
                .grant(Permission.CRU, UserRole.HEARING_CENTRE_ADMIN)
                .grant(Permission.CRU, UserRole.PCS_SOLICITOR)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary()
                .endButtonLabel("Submit");

        PageBuilder pageBuilder = new PageBuilder(eventBuilder);
        selectDocumentPage.addTo(pageBuilder);
        amendDocumentDetailsPage.addTo(pageBuilder);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        if (caseData.getDocumentAmendDetails() == null) {
            caseData.setDocumentAmendDetails(new DocumentAmendDetails());
        }
        documentAmendSelectionService.initialise(
            eventPayload.caseReference(),
            caseData,
            caseData.getDocumentAmendDetails()
        );
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        long caseReference = eventPayload.caseReference();
        DocumentAmendService.AmendedDocument amendedDocument = documentAmendService
            .amendDocument(caseData, caseReference);

        String address = addressFormatter
            .formatMediumAddress(caseData.getPropertyAddress(), AddressFormatter.COMMA_DELIMITER);

        return SubmitResponse.<State>builder()
            .confirmationBody(getConfirmationBody(
                amendedDocument.fileName(),
                caseReference,
                address,
                amendedDocument.partyName()
            ))
            .build();
    }

    private String getConfirmationBody(String fileName, long caseReference, String address, String partyName) {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-32">Document %s amended</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">Case number #%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
            </div>

            <h3>What happens next</h3>

            The amended document is available to view in case file view.
            """.formatted(fileName, caseReference, address, partyName);
    }
}
