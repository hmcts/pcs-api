package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.amenddocument;

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
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.documentamend.AmendDocumentDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworker.documentamend.SelectDocumentPage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentSelectionService;
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ManageDocumentStates.MANAGE_DOCUMENT_STATES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.amendDocuments;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;

@Component
@AllArgsConstructor
public class AmendDocuments implements CCDConfig<PCSCase, State, UserRole> {

    private final DocumentSelectionService documentSelectionService;
    private final DocumentAmendService documentAmendService;
    private final ConfirmationPanelBuilder confirmationPanelBuilder;
    private final SelectDocumentPage selectDocumentPage;
    private final AmendDocumentDetailsPage amendDocumentDetailsPage;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(amendDocuments.name(), this::submit, this::start)
                .forStates(MANAGE_DOCUMENT_STATES)
                .nonConcurrent()
                .name("Manage documents: Amend")
                .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_2, CASEWORKER_EVENTS))
                .grant(Permission.CRU, CASEWORKER_ROLES)
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
        documentSelectionService.initialise(
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

        return SubmitResponse.<State>builder()
            .confirmationBody(getConfirmationBody(
                caseData,
                amendedDocument.fileName(),
                caseReference
            ))
            .build();
    }

    private String getConfirmationBody(PCSCase caseData, String fileName, long caseReference) {

        String confirmationPanel = confirmationPanelBuilder.buildStandardConfirmationPanel(
            "Document %s amended".formatted(fileName), caseData, caseReference);

        return """
            ---
            %s

            <h3>What happens next</h3>

            The amended document is available to view in case file view.
            """.formatted(confirmationPanel);
    }
}
