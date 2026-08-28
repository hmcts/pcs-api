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
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

@Component("caseworkerUploadDocumentSubmitHandler")
@RequiredArgsConstructor
public class SubmitHandler implements Submit<PCSCase, State> {

    private final CaseworkerDocumentService caseworkerDocumentService;
    private final AddressFormatter addressFormatter;

    public SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        CaseworkerDocument caseworkerDocument = caseData.getCaseworkerDocument();
        DocumentEntity documentEntity = caseworkerDocumentService.saveNewDocument(caseworkerDocument, caseReference);

        String modifiedFilename = FilenameUtils.getBaseName(documentEntity.getFileName());

        String address = addressFormatter
            .formatMediumAddress(caseData.getPropertyAddress(), AddressFormatter.COMMA_DELIMITER);

        return SubmitResponse.<State>builder()
            .confirmationBody(getConfirmationBody(modifiedFilename, caseReference,
                                                  address, caseData.getCaseNameHmctsInternal()))
            .build();

    }

    private String getConfirmationBody(String modifiedFilename, long caseReference, String address, String caseName) {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-32">‘%s’ uploaded</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">Case number #%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
            <span class="govuk-panel__body govuk-!-font-size-24">%s</span><br>
            </div>

            <h3>What happens next</h3>

            The document is available to view in case file view.
            """.formatted(modifiedFilename, caseReference, address, caseName);
    }

}
