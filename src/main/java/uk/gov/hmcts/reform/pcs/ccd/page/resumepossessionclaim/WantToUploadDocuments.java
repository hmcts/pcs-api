package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocuments;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class WantToUploadDocuments implements CcdPageConfiguration {

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
        AdditionalDocuments additionalDocuments = new AdditionalDocuments();
        LegislativeCountry legislativeCountry = caseData.getLegislativeCountry();

        additionalDocuments.setDocumentTypeList(createAdditionalDocumentList(legislativeCountry));
        caseData.setAdditionalDocs(new ArrayList<>());
        caseData.getAdditionalDocs().add(ListValue.<AdditionalDocuments>builder()
                .value(additionalDocuments)
                .build());

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
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
        if (country.equals(LegislativeCountry.ENGLAND)) {
            return AdditionalDocumentType.isEnglandSpecific(dt) || !AdditionalDocumentType.isWalesSpecific(dt);
        } else {
            return AdditionalDocumentType.isWalesSpecific(dt) || !AdditionalDocumentType.isEnglandSpecific(dt);
        }
    }
}
