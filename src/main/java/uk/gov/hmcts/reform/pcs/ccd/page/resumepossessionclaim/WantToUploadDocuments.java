package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Component
public class WantToUploadDocuments implements CcdPageConfiguration {

    private DraftCaseDataService draftCaseDataService;

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

        if (caseData.getWantToUploadDocuments().equals(VerticalYesNo.YES)
            && CollectionUtils.isEmpty(caseData.getAdditionalDocuments())) {
            AdditionalDocument additionalDocuments = new AdditionalDocument();
            LegislativeCountry legislativeCountry = caseData.getLegislativeCountry();
            additionalDocuments.setDocumentType(createAdditionalDocumentList(legislativeCountry));
            caseData.setAdditionalDocuments(new ArrayList<>());
            caseData.getAdditionalDocuments().add(ListValue.<AdditionalDocument>builder()
                    .value(additionalDocuments)
                    .build());
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private void setAdditionalDocumentsFromDraft(long caseReference, PCSCase caseData) {
        Optional<PCSCase> draftCaseData =
                draftCaseDataService.getUnsubmittedCaseData(caseReference, EventId.resumePossessionClaim);

        if (draftCaseData.isPresent() && draftCaseData.get().getAdditionalDocuments() != null) {
            caseData.setAdditionalDocuments(draftCaseData.get().getAdditionalDocuments());
        }
    }

    private DynamicList createAdditionalDocumentList(LegislativeCountry legislativeCountry) {

        DynamicList documentTypeList = new DynamicList(null, new ArrayList<>());

        for (AdditionalDocumentType dt : AdditionalDocumentType.values()) {
            if (canAddDocumentType(dt, legislativeCountry)) {
                DynamicListElement element = new DynamicListElement(UUID.randomUUID(), dt.getLabel());
                documentTypeList.getListItems().add(element);
            }
        }

        return documentTypeList;
    }

    private boolean canAddDocumentType(AdditionalDocumentType dt, LegislativeCountry country) {
        return dt.isApplicableFor(country);
    }
}
