package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.DocumentUpload;
import uk.gov.hmcts.reform.pcs.ccd.page.createtestcase.MakeAClaim;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.uploadDocumentPoc;

import java.util.List;

@Component
@AllArgsConstructor
public class UploadDocumentPoc implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(uploadDocumentPoc.name(), this::submit, this::start)
                .initialState(State.CASE_ISSUED)
                .name("Upload Document - POC")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER);

        new PageBuilder(eventBuilder)
            .add(new ClaimantInformation())
            .add(new MakeAClaim())
            .add(new DocumentUpload());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        caseData.setApplicantForename("Preset value");

        return caseData;
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setPaymentStatus(PaymentStatus.UNPAID);

        List<ListValue<Document>> supportingDocuments = pcsCase.getSupportingDocuments();

        if (supportingDocuments != null) {
            for (ListValue<Document> documentWrapper : supportingDocuments) {
                if (documentWrapper != null && documentWrapper.getValue() != null) {
                    Document document = documentWrapper.getValue();
                    String fileName = document.getFilename();
                    String filePath = document.getUrl();


                    pcsCaseService.addDocumentToCase(caseReference, fileName, filePath);
                }
            }
        }


        pcsCaseService.createCase(caseReference, pcsCase);
    }
}
