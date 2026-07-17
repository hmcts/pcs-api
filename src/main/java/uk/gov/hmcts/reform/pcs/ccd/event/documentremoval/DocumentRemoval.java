package uk.gov.hmcts.reform.pcs.ccd.event.documentremoval;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.documentremoval.DocumentRemovalDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.documentremoval.SelectDocumentToRemovePage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentAmendSelectionService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentRemovalService;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ManageDocumentsStates.MANAGE_DOCUMENTS_STATES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.removeDocuments;

@Component
@AllArgsConstructor
public class DocumentRemoval implements CCDConfig<PCSCase, State, UserRole> {

    private final DocumentAmendSelectionService documentSelectionService;
    private final DocumentRemovalService documentRemovalService;
    private final SelectDocumentToRemovePage selectDocumentToRemovePage;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(removeDocuments.name(), this::submit, this::start)
                .forStates(MANAGE_DOCUMENTS_STATES)
                .name("Manage documents: Remove")
                .grant(Permission.CRU, CASEWORKER_ROLES)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary()
                .endButtonLabel("Submit");

        PageBuilder pageBuilder = new PageBuilder(eventBuilder);
        selectDocumentToRemovePage.addTo(pageBuilder);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        if (caseData.getDocumentRemovalDetails() == null) {
            caseData.setDocumentRemovalDetails(new DocumentRemovalDetails());
        }
        DocumentRemovalDetails details = caseData.getDocumentRemovalDetails();

        documentSelectionService.initialise(eventPayload.caseReference(), caseData, details);
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        DocumentRemovalDetails details = eventPayload.caseData().getDocumentRemovalDetails();
        String reason = details.getReasonForCategory(details.getSelectedFolder());

        documentRemovalService.removeDocument(UUID.fromString(details.getSelectedDocumentId()), reason);

        return SubmitResponse.<State>builder()
            .confirmationBody(buildConfirmationMarkdown(details, eventPayload.caseReference()))
            .build();
    }

    private String buildConfirmationMarkdown(DocumentRemovalDetails details, long caseReference) {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">%s removed </span><br>
            <span class="govuk-panel__body">Case number: %s</span><br>
            <span class="govuk-panel__body">Property address: %s</span>
            </div>

            <h3 class="govuk-heading-s">What happens next</h3>
            <p class="govuk-body govuk-!-margin-bottom-6">The document will no longer appear in case file view</p>
            """.formatted(
                details.getSelectedDocumentFileName(), caseReference, details.getPropertyAddressSummary());
    }
}
