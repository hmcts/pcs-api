package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.Optional;

@AllArgsConstructor
@Component
public class WantToUploadDocuments implements CcdPageConfiguration {

    private final DraftCaseDataService draftCaseDataService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("wantToUploadDocuments", this::midEvent)
            .pageLabel("Upload additional documents")
            .label("wantToUploadDocuments-separator", "---")
            .mandatory(PCSCase::getWantToUploadDocuments)
            .label("wantToUploadDocuments-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        setAdditionalDocumentsFromDraft(details.getId(), caseData);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private void setAdditionalDocumentsFromDraft(long caseReference, PCSCase caseData) {
        Optional<PCSCase> draftCaseData =
                draftCaseDataService.getUnsubmittedCaseData(caseReference, EventId.resumePossessionClaim);

        if (draftCaseData.isPresent() && draftCaseData.get().getAdditionalDocuments() != null) {
            caseData.setAdditionalDocuments(draftCaseData.get().getAdditionalDocuments());

            for (ListValue<AdditionalDocument> additionalDocumentListValue : caseData.getAdditionalDocuments()) {
                AdditionalDocument additionalDocument = additionalDocumentListValue.getValue();
                if (additionalDocument == null || additionalDocument.getDocumentType() == null) {
                    continue;
                }

                String label = additionalDocument.getDocumentType().getValueLabel();
                if (label == null) {
                    continue;
                }

                if (caseData.getLegislativeCountry() == LegislativeCountry.WALES) {
                    additionalDocument.setDocumentTypeWales(AdditionalDocumentTypeWales.getValueFromLabel(label));
                } else {
                    additionalDocument.setDocumentTypeEngland(AdditionalDocumentTypeEngland.getValueFromLabel(label));
                }

                additionalDocument.setDocumentType(null);
            }
        }
    }

}
