package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Component
public class UploadAdditionalDocumentsDetails implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;
    private static final String DESCRIPTION_LABEL = "short description";

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadAdditionalDocuments", this::midEvent)
            .pageLabel("Upload additional documents")
            .showCondition("wantToUploadDocuments=\"YES\"")

            // ---------- Horizontal separator ----------
            .label("uploadAdditionalDocuments-separator", "---")
                .label("uploadAdditionalDocuments-separator-help",
                       """
                       <p class="govuk-body govuk-!-font-size-19">
                       You must select the type of document you’re uploading and give it a short description.
                       </p>
                       """
                )
            .label("uploadAdditionalDocuments-heading",
                   """
                   <h2>Before you upload your documents</h2>
                   <p class="govuk-body govuk-!-font-size-19">Give your document a name that explains what it is.</p>
                   """
            )
             .list(PCSCase::getAdditionalDocuments)
                .mandatory(AdditionalDocument::getDocumentType, ShowConditions.NEVER_SHOW)
                .mandatory(AdditionalDocument::getDocumentTypeEngland, ShowConditions.ENGLAND)
                .mandatory(AdditionalDocument::getDocumentTypeWales, ShowConditions.WALES)
                .mandatory(AdditionalDocument::getDocument)
                .mandatory(AdditionalDocument::getDescription)
            .done()
            //.mandatory(PCSCase::getAdditionalDocuments)
            .label("uploadAdditionalDocuments-saveAndReturn", CommonPageContent.SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        copyDocumentType(caseData);

        List<String> errors = validateDocumentDescription(caseData.getAdditionalDocuments(), DESCRIPTION_LABEL);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
            .data(caseData)
            .build();
    }

    private List<String> validateDocumentDescription(
        List<ListValue<AdditionalDocument>> additionalDocuments,
        String sectionLabel) {

        List<String> validationErrors = new ArrayList<>();

        for (int i = 0; i < additionalDocuments.size(); i++) {
            String docDescription = additionalDocuments.get(i).getValue().getDescription();
            String sectionHint = "Additional document %d".formatted(i + 1) + "'s " + sectionLabel;
            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                docDescription, sectionHint, TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT)
            );
        }
        return validationErrors;
    }

    public void copyDocumentType(PCSCase caseData) {
        if (caseData.getAdditionalDocuments() == null) {
            return;
        }

        for (ListValue<AdditionalDocument> additionalDocumentListValue : caseData.getAdditionalDocuments()) {
            AdditionalDocument additionalDocument = additionalDocumentListValue.getValue();
            if (additionalDocument == null) {
                continue;
            }

            if (caseData.getLegislativeCountry() == LegislativeCountry.WALES) {
                AdditionalDocumentTypeWales walesType = additionalDocument.getDocumentTypeWales();
                if (walesType != null) {
                    additionalDocument.setDocumentType(
                        createDynamicListForDocumentType(walesType.getLabel(), AdditionalDocumentTypeWales.values())
                    );
                    additionalDocument.setDocumentTypeWales(null);
                }
            } else {
                AdditionalDocumentTypeEngland englandType = additionalDocument.getDocumentTypeEngland();
                if (englandType != null) {
                    additionalDocument.setDocumentType(
                        createDynamicListForDocumentType(englandType.getLabel(), AdditionalDocumentTypeEngland.values())
                    );
                    additionalDocument.setDocumentTypeEngland(null);
                }
            }
        }
    }

    private DynamicList createDynamicListForDocumentType(String label, HasLabel[] documentTypes) {
        List<DynamicListElement> items = Arrays.stream(documentTypes)
            .map(documentType -> new DynamicListElement(UUID.randomUUID(), documentType.getLabel()))
            .toList();

        DynamicListElement selectedItem = items.stream()
            .filter(item -> label.equals(item.getLabel()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No document type found for label: " + label));

        return new DynamicList(selectedItem, items);
    }

}
