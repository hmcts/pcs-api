package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
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
        Optional<PCSCase> draftCaseData = draftCaseDataService.getUnsubmittedCaseData(
            caseReference,
            EventId.resumePossessionClaim
        );

        if (draftCaseData.isEmpty()) {
            return;
        }

        PCSCase draftCase = draftCaseData.get();
        LegislativeCountry legislativeCountry = caseData.getLegislativeCountry();

        if (legislativeCountry == LegislativeCountry.WALES) {
            caseData.setAdditionalDocumentsWales(draftCase.getAdditionalDocumentsWales());

            if (draftCase.getAdditionalDocuments() != null && caseData.getAdditionalDocumentsWales() == null) {
                caseData.setAdditionalDocumentsWales(mapAdditionalDocumentsWales(draftCase.getAdditionalDocuments()));
            }
        } else {
            caseData.setAdditionalDocumentsEngland(draftCase.getAdditionalDocumentsEngland());

            if (draftCase.getAdditionalDocuments() != null && caseData.getAdditionalDocumentsEngland() == null) {
                caseData.setAdditionalDocumentsEngland(
                    mapAdditionalDocumentsEngland(draftCase.getAdditionalDocuments())
                );
            }
        }
    }

    private List<ListValue<AdditionalDocumentEngland>> mapAdditionalDocumentsEngland(
        List<ListValue<AdditionalDocument>> additionalDocuments
    ) {
        return additionalDocuments.stream()
            .map(additionalDocument -> ListValue.<AdditionalDocumentEngland>builder()
                .id(getDocumentId(additionalDocument))
                .value(AdditionalDocumentEngland.builder()
                    .documentType(mapAdditionalDocumentTypeEngland(additionalDocument.getValue()))
                    .document(additionalDocument.getValue().getDocument())
                    .description(additionalDocument.getValue().getDescription())
                    .build())
                .build())
            .toList();
    }

    private List<ListValue<AdditionalDocumentWales>> mapAdditionalDocumentsWales(
        List<ListValue<AdditionalDocument>> additionalDocuments
    ) {
        return additionalDocuments.stream()
            .map(additionalDocument -> ListValue.<AdditionalDocumentWales>builder()
                .id(getDocumentId(additionalDocument))
                .value(AdditionalDocumentWales.builder()
                    .documentType(mapAdditionalDocumentTypeWales(additionalDocument.getValue()))
                    .document(additionalDocument.getValue().getDocument())
                    .description(additionalDocument.getValue().getDescription())
                    .build())
                .build())
            .toList();
    }

    private AdditionalDocumentTypeEngland mapAdditionalDocumentTypeEngland(AdditionalDocument additionalDocument) {
        AdditionalDocumentType additionalDocumentType = getAdditionalDocumentType(additionalDocument);
        return additionalDocumentType == null ? null : 
            AdditionalDocumentTypeEngland.valueOf(additionalDocumentType.name());
    }

    private AdditionalDocumentTypeWales mapAdditionalDocumentTypeWales(AdditionalDocument additionalDocument) {
        AdditionalDocumentType additionalDocumentType = getAdditionalDocumentType(additionalDocument);
        return additionalDocumentType == null ? null : 
            AdditionalDocumentTypeWales.valueOf(additionalDocumentType.name());
    }

    private AdditionalDocumentType getAdditionalDocumentType(AdditionalDocument additionalDocument) {
        if (additionalDocument == null
            || additionalDocument.getDocumentType() == null
            || additionalDocument.getDocumentType().getValue() == null) {
            return null;
        }

        return AdditionalDocumentType.getValueFromLabel(additionalDocument.getDocumentType().getValue().getLabel());
    }

    private String getDocumentId(ListValue<AdditionalDocument> additionalDocument) {
        return additionalDocument.getId() != null ? additionalDocument.getId() : null;
    }
}
