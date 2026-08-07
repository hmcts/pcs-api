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
import uk.gov.hmcts.reform.pcs.ccd.page.documentamend.AmendDocumentDetailsPlaceholderPage;
import uk.gov.hmcts.reform.pcs.ccd.page.documentamend.SelectDocumentPage;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentSelectionService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.amendDocuments;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.CASEWORKER_EVENTS;
import static uk.gov.hmcts.reform.pcs.service.FeatureFlag.RELEASE_1_DOT_2;

@Component
@AllArgsConstructor
public class AmendDocuments implements CCDConfig<PCSCase, State, UserRole> {

    private final DocumentSelectionService documentSelectionService;
    private final SelectDocumentPage selectDocumentPage;
    private final AmendDocumentDetailsPlaceholderPage amendDocumentDetailsPlaceholderPage;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(amendDocuments.name(), this::submit, this::start)
                .forStates(State.CASE_ISSUED)
                .name("Manage documents: Amend")
                .showCondition(ShowConditions.featureFlagsEnabled(RELEASE_1_DOT_2, CASEWORKER_EVENTS))
                .grant(Permission.CRU, CASEWORKER_ROLES)
                .endButtonLabel("Submit");

        PageBuilder pageBuilder = new PageBuilder(eventBuilder);
        selectDocumentPage.addTo(pageBuilder);
        amendDocumentDetailsPlaceholderPage.addTo(pageBuilder);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        if (caseData.getDocumentAmendDetails() == null) {
            caseData.setDocumentAmendDetails(new DocumentAmendDetails());
        }
        DocumentAmendDetails details = caseData.getDocumentAmendDetails();

        documentSelectionService.initialise(eventPayload.caseReference(), caseData, details);
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.defaultResponse();
    }
}
