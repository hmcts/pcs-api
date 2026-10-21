package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.updatedocument;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentCategoryUpdateService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DocumentMoveRoles.DOCUMENT_MOVE_ROLES;

@Component
@RequiredArgsConstructor
public class DocumentUpdated implements CCDConfig<PCSCase, State, UserRole> {

    private final DocumentCategoryUpdateService documentCategoryUpdateService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder.decentralisedEvent(EventId.documentUpdated.name(), this::submit)
            .forAllStates()
            .name("Document updated")
            .grantHistoryOnly(DOCUMENT_MOVE_ROLES);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> payload) {
        documentCategoryUpdateService.updateCategories(payload.caseReference(), payload.caseData().getAllDocuments());
        return SubmitResponse.defaultResponse();
    }
}
