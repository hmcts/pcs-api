package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.addrentstatement.UploadDocument;
import uk.gov.hmcts.reform.pcs.ccd.service.DocumentService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType.RENT_STATEMENT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.addRentStatement;

@Slf4j
@Component
@AllArgsConstructor
public class AddRentStatement implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final DocumentService documentService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(addRentStatement.name(), this::submit)
                .forStates(PENDING_CASE_ISSUED, CASE_ISSUED)
                .name("Upload amended rent statement")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();

        new PageBuilder(eventBuilder)
            .add(new UploadDocument());

    }


    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        Document amendedRentStatement = caseData.getAmendedRentStatement();
        VerticalYesNo isAmendedDocument = caseData.getIsAmendedDocument();

        List<DocumentEntity> documentEntities
            = documentService.saveDocument(amendedRentStatement, RENT_STATEMENT, isAmendedDocument);

        pcsCaseEntity.addDocuments(documentEntities);
        pcsCaseEntity.getClaims().getFirst().addClaimDocuments(documentEntities);

        return SubmitResponse.<State>builder().build();
    }

}
