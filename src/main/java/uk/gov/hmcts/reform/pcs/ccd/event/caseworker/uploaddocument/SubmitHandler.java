package uk.gov.hmcts.reform.pcs.ccd.event.caseworker.uploaddocument;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.Submit;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.documentupload.CaseworkerDocument;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.caseworker.CaseworkerDocumentService;
import uk.gov.hmcts.reform.pcs.ccd.util.ConfirmationPanelBuilder;

@Component("caseworkerUploadDocumentSubmitHandler")
@RequiredArgsConstructor
public class SubmitHandler implements Submit<PCSCase, State> {

    private final CaseworkerDocumentService caseworkerDocumentService;
    private final ConfirmationPanelBuilder confirmationPanelBuilder;

    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        CaseworkerDocument caseworkerDocument = caseData.getCaseworkerDocument();
        DocumentEntity documentEntity = caseworkerDocumentService.saveNewDocument(caseworkerDocument, caseReference);

        String modifiedFilename = FilenameUtils.getBaseName(documentEntity.getFileName());

        return SubmitResponse.<State>builder()
            .confirmationBody(getConfirmationBody(modifiedFilename, caseReference, caseData))
            .build();

    }

    private String getConfirmationBody(String modifiedFilename, long caseReference, PCSCase caseData) {

        String confirmationPanel = confirmationPanelBuilder
            .buildStandardConfirmationPanel("‘%s’ uploaded".formatted(modifiedFilename), caseData, caseReference);

        return """
            ---
            %s

            <h3>What happens next</h3>

            The document is available to view in case file view.
            """.formatted(confirmationPanel);
    }

}
