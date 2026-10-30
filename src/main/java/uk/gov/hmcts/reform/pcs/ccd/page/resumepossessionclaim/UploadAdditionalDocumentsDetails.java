package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.ENGLAND;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
//import java.util.function.Function;

@AllArgsConstructor
@Component
public class UploadAdditionalDocumentsDetails implements CcdPageConfiguration {

    private final TextAreaValidationService textAreaValidationService;
    private final FileUploadValidationService fileUploadValidationService;
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
            .readonly(PCSCase::getLegislativeCountry, NEVER_SHOW, true)
            .list(PCSCase::getAdditionalDocuments)
                .readonly(AdditionalDocument::getDocumentType)
                .mandatory(AdditionalDocument::getDocumentTypeEngland, ENGLAND)
                .mandatory(AdditionalDocument::getDocumentTypeWales, WALES)
                .mandatory(AdditionalDocument::getDocument)
                .mandatory(AdditionalDocument::getDescription)
            .done()
            .label("uploadAdditionalDocuments-saveAndReturn", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        copyDocumentType(caseData);

        List<String> errors = validateDocumentDescription(caseData.getAdditionalDocuments(), DESCRIPTION_LABEL);

        errors.addAll(fileUploadValidationService.validateRequiredAdditionalDocuments(
            caseData.getAdditionalDocuments(), FileUploadValidationService.ADDITIONAL_DOCUMENT_REQUIRED));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
            .data(caseData)
            .build();
    }

    public void copyDocumentType(PCSCase caseData) {
        if (caseData.getAdditionalDocuments() == null) {
            return;
        }

        List<DynamicListElement> documentTypes = getDocumentTypes(caseData);

        for (ListValue<AdditionalDocument> additionalDocumentListValue : caseData.getAdditionalDocuments()) {
            AdditionalDocument additionalDocument = additionalDocumentListValue.getValue();
            if (additionalDocument == null || additionalDocument.getDocumentType() != null) {
                continue;
            }

            if (caseData.getLegislativeCountry() == LegislativeCountry.WALES) {
                AdditionalDocumentTypeWales walesType = additionalDocument.getDocumentTypeWales();
                if (walesType != null) {
                    additionalDocument.setDocumentType(
                        createDynamicListForDocumentType(walesType.getLabel(), documentTypes)
                    );
                }
            } else {
                AdditionalDocumentTypeEngland englandType = additionalDocument.getDocumentTypeEngland();
                if (englandType != null) {
                    additionalDocument.setDocumentType(
                        createDynamicListForDocumentType(englandType.getLabel(), documentTypes)
                    );
                }
            }
        }
    }

    private List<DynamicListElement> getDocumentTypes(PCSCase caseData) {
        ListValue<AdditionalDocument> firstAdditionalDocument = caseData.getAdditionalDocuments().isEmpty()
            ? null
            : caseData.getAdditionalDocuments().getFirst();

        if (firstAdditionalDocument != null
            && firstAdditionalDocument.getValue() != null
            && firstAdditionalDocument.getValue().getDocumentType() != null
            && firstAdditionalDocument.getValue().getDocumentType().getListItems() != null) {
            return firstAdditionalDocument.getValue().getDocumentType().getListItems();
        }

        List<DynamicListElement> documentTypes = createDocumentTypeItems(
            caseData.getLegislativeCountry() == LegislativeCountry.WALES
            ? AdditionalDocumentTypeWales.values()
            : AdditionalDocumentTypeEngland.values());

        return documentTypes;
    }

    private List<DynamicListElement> createDocumentTypeItems(HasLabel[] documentTypes) {
        return Arrays.stream(documentTypes)
            .map(documentType -> new DynamicListElement(
                UUID.nameUUIDFromBytes(documentType.getLabel().getBytes(StandardCharsets.UTF_8)),
                documentType.getLabel()
            ))
            .toList();
    }

    private DynamicList createDynamicListForDocumentType(String label, List<DynamicListElement> documentTypes) {
        DynamicListElement selectedItem = documentTypes.stream()
            .filter(item -> label.equals(item.getLabel()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No document type found for label: " + label));

        return new DynamicList(selectedItem, documentTypes);
    }
  
    private List<String> validateDocumentDescription(
        List<ListValue<AdditionalDocument>> additionalDocuments,
        String sectionLabel) {

        List<String> validationErrors = new ArrayList<>();

        if (additionalDocuments == null) {
            return validationErrors;
        }

        for (int i = 0; i < additionalDocuments.size(); i++) {
            String docDescription = additionalDocuments.get(i).getValue().getDescription();
            String sectionHint = "Additional document %d".formatted(i + 1) + "'s " + sectionLabel;
            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                docDescription, sectionHint, TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT)
            );
        }
        return validationErrors;
    }
}
