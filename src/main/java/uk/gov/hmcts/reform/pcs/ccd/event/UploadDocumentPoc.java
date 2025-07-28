package uk.gov.hmcts.reform.pcs.ccd.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.DocumentUpload;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.uploadDocumentPoc;

@Component
public class UploadDocumentPoc implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(uploadDocumentPoc.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Upload Document - POC")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER);

        new PageBuilder(eventBuilder)
            .add(new DocumentUpload());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        return eventPayload.caseData();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
    }
}
