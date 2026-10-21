package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
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

        if (VerticalYesNo.NO.equals(caseData.getWantToUploadDocuments())) {
            caseData.setAdditionalDocuments(null);
            caseData.setDocumentsYouveUploaded(null);
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
        }

        setAdditionalDocumentsFromDraft(details.getId(), caseData);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private void setAdditionalDocumentsFromDraft(long caseReference, PCSCase caseData) {
        Optional<PCSCase> draftCaseData = draftCaseDataService.getUnsubmittedCaseData(
            caseReference,
            EventId.resumePossessionClaim
        );

        if (draftCaseData.isEmpty()) {
            return;
        }

        PCSCase draftCase = draftCaseData.get();
        copyDocumentTypes(draftCase, caseData.getLegislativeCountry());
        caseData.setAdditionalDocuments(draftCase.getAdditionalDocuments());
    }

    private void copyDocumentTypes(PCSCase draftCase, LegislativeCountry legislativeCountry) {
        if (draftCase.getAdditionalDocuments() == null) {
            return;
        }

        draftCase.getAdditionalDocuments().stream()
            .map(listValue -> listValue.getValue())
            .filter(document -> document != null && document.getDocumentType() != null)
            .filter(document -> document.getDocumentType().getValue() != null)
            .forEach(document -> copyDocumentType(document, legislativeCountry));
    }

    private void copyDocumentType(AdditionalDocument document, LegislativeCountry legislativeCountry) {
        AdditionalDocumentType documentType = AdditionalDocumentType.getValueFromLabel(
            document.getDocumentType().getValue().getLabel()
        );

        if (legislativeCountry == LegislativeCountry.WALES) {
            document.setDocumentTypeWales(AdditionalDocumentTypeWales.valueOf(documentType.name()));
        } else {
            document.setDocumentTypeEngland(AdditionalDocumentTypeEngland.valueOf(documentType.name()));
        }
    }
}
