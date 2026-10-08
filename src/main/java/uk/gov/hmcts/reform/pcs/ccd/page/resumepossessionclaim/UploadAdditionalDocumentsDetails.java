package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentEngland;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.FileUploadValidationService;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;
import uk.gov.hmcts.reform.pcs.ccd.util.StringUtils;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.WALES;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.ENGLAND;
import static uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent.SAVE_AND_RETURN;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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
            .mandatory(PCSCase::getAdditionalDocumentsEngland, ENGLAND)
            .mandatory(PCSCase::getAdditionalDocumentsWales, WALES)
            .label("uploadAdditionalDocuments-saveAndReturn", SAVE_AND_RETURN);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        List<String> errors = new ArrayList<>(validateDocumentDescription(caseData, DESCRIPTION_LABEL));

        errors.addAll(fileUploadValidationService.validateRequiredAdditionalDocuments(
            getAdditionalDocuments(caseData), FileUploadValidationService.ADDITIONAL_DOCUMENT_REQUIRED));

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .errorMessageOverride(StringUtils.joinIfNotEmpty("\n", errors))
            .data(caseData)
            .build();
    }

    private List<ListValue<AdditionalDocument>> getAdditionalDocuments(PCSCase caseData) {
        if (caseData.getLegislativeCountry() == LegislativeCountry.ENGLAND) {
            return toAdditionalDocuments(
                caseData.getAdditionalDocumentsEngland(), AdditionalDocumentEngland::getDocument
            );
        }

        if (caseData.getLegislativeCountry() == LegislativeCountry.WALES) {
            return toAdditionalDocuments(caseData.getAdditionalDocumentsWales(), AdditionalDocumentWales::getDocument);
        }

        return caseData.getAdditionalDocuments();
    }

    private <T> List<ListValue<AdditionalDocument>> toAdditionalDocuments(
        List<ListValue<T>> documents,
        Function<T, Document> documentExtractor
    ) {
        if (documents == null) {
            return null;
        }

        return documents.stream()
            .map(item -> ListValue.<AdditionalDocument>builder()
                .id(item.getId())
                .value(AdditionalDocument.builder()
                    .document(item.getValue() == null ? null : documentExtractor.apply(item.getValue()))
                    .build())
                .build())
            .toList();
    }

    private List<String> validateDocumentDescription(
        PCSCase caseData,
        String sectionLabel) {

        if (caseData.getLegislativeCountry() == LegislativeCountry.ENGLAND) {
            return validateDocumentDescriptions(
                caseData.getAdditionalDocumentsEngland(),
                AdditionalDocumentEngland::getDescription,
                sectionLabel
            );
        }

        if (caseData.getLegislativeCountry() == LegislativeCountry.WALES) {
            return validateDocumentDescriptions(
                caseData.getAdditionalDocumentsWales(),
                AdditionalDocumentWales::getDescription,
                sectionLabel
            );
        }

        return List.of();
    }

    private <T> List<String> validateDocumentDescriptions(
        List<ListValue<T>> additionalDocuments,
        Function<T, String> descriptionExtractor,
        String sectionLabel
    ) {
        List<String> validationErrors = new ArrayList<>();

        if (additionalDocuments == null) {
            return validationErrors;
        }

        for (int i = 0; i < additionalDocuments.size(); i++) {
            String docDescription = descriptionExtractor.apply(additionalDocuments.get(i).getValue());
            String sectionHint = "Additional document %d".formatted(i + 1) + "'s " + sectionLabel;
            validationErrors.addAll(textAreaValidationService.validateSingleTextArea(
                docDescription, sectionHint, TextAreaValidationService.EXTRA_SHORT_TEXT_LIMIT)
            );
        }

        return validationErrors;
    }
}
