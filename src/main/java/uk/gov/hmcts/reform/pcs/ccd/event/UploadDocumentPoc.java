package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.DocumentUpload;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.uploadDocumentPoc;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class UploadDocumentPoc implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(uploadDocumentPoc.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Upload Document - POC")
                .description("Create new case and upload documents")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER)
                .showSummary();

        new PageBuilder(eventBuilder)
            .add(new ClaimantInformation())
            .add(new DocumentUpload());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        String userDetails = securityContextService.getCurrentUserDetails().getSub();

        caseData.setClaimantName(userDetails);

        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Creating document upload POC case: {}", caseReference);

        List<ListValue<Document>> supportingDocuments = pcsCase.getSupportingDocuments();
        if (supportingDocuments != null) {
            for (ListValue<Document> documentWrapper : supportingDocuments) {
                if (documentWrapper != null && documentWrapper.getValue() != null) {
                    Document document = documentWrapper.getValue();
                    pcsCaseService.addDocumentToCase(caseReference, document.getFilename(), document.getUrl());
                }
            }
        }

        pcsCaseService.createCase(caseReference, pcsCase);

        log.info("Successfully created case: {}", caseReference);
    }
}
