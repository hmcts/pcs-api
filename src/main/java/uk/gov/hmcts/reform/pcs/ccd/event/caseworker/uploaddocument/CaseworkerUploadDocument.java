package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.uploaddocument;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.caseworkeruploaddocument.UploadADocument;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerRoles.CASEWORKER_ROLES;


@Component
@Slf4j
public class CaseworkerUploadDocument implements CCDConfig<PCSCase, State, UserRole> {

    private final StartHandler startHandler;
    private final SubmitHandler submitHandler;

    public CaseworkerUploadDocument(@Qualifier("caseworkerUploadDocumentStartHandler") StartHandler startHandler,
                                    @Qualifier("caseworkerUploadDocumentSubmitHandler") SubmitHandler submitHandler) {

        this.startHandler = startHandler;
        this.submitHandler = submitHandler;
    }

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(EventId.caseworkerUploadDocuments.name(), submitHandler, startHandler)
            .forAllStates()
            .name("Manage documents: Upload")
            .description("Upload a document to the case")
            .showSummary()
            .grant(Permission.CRU, CASEWORKER_ROLES);

        new PageBuilder(eventBuilder)
            .add(new UploadADocument());
    }

}
