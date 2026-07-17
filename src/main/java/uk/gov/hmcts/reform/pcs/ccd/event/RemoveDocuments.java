package uk.gov.hmcts.reform.pcs.ccd.event;

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

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.removeDocuments;

@Component
@AllArgsConstructor
public class RemoveDocuments implements CCDConfig<PCSCase, State, UserRole> {

    private final DocumentAmendSelectionService documentSelectionService;
    private final DocumentRemovalService documentRemovalService;
    private final SelectDocumentToRemovePage selectDocumentToRemovePage;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(removeDocuments.name(), this::submit, this::start)
                .forStates(State.CASE_ISSUED)
                .name("Manage documents: Remove")
                .grant(Permission.CRU, UserRole.HEARING_CENTRE_ADMIN)
                .grant(Permission.CRU, UserRole.HEARING_CENTRE_TEAM_LEADER)
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

        return SubmitResponse.defaultResponse();
    }
}
