package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Service for validating uploaded documents against the allowed file type allowlist.
 * Any file whose extension is not in {@link #ALLOWED_FILE_EXTENSIONS} is rejected.
 */
@Service
public class FileUploadValidationService {

    public static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(
        "doc", "dot", "docx", "dotx",
        "xls", "xlt", "xla", "xlsx", "xltx", "xlsb",
        "ppt", "pot", "pps", "ppa", "pptx", "potx", "ppsx",
        "pdf", "txt", "rtf", "csv",
        "jpg", "jpeg", "png", "bmp", "tif", "tiff");
    public static final String DISALLOWED_FILE_TYPE_ERROR = "Your upload contains a disallowed file type";
    public static final String ALLOWED_FILE_TYPE_GUIDANCE =
        "The selected file must be a DOC/DOT/DOCX/DOTX, XLS/XLT/XLA/XLSX/XLTX/XLSB, "
        + "PPT/POT/PPS/PPA/PPTX/POTX/PPSX, PDF, TXT/RTF/CSV, JPG/JPEG, PNG, BMP, TIF/TIFF";

    // Messages shown when a required upload is missing. Held here so each page and its tests reference a
    // single source of truth rather than repeating the literal text.
    public static final String NOTICE_DOCUMENT_REQUIRED =
        "You must upload a copy of the notice served";
    public static final String TENANCY_LICENCE_DOCUMENT_REQUIRED =
        "You must upload a copy of the tenancy or licence agreement";
    public static final String RENT_STATEMENT_REQUIRED =
        "You must upload the rent statement";
    public static final String ADDITIONAL_DOCUMENT_REQUIRED =
        "You must upload a document";
    public static final String ENERGY_PERFORMANCE_CERTIFICATE_REQUIRED =
        "You must upload a copy of the energy performance certificate";
    public static final String GAS_SAFETY_REPORT_REQUIRED =
        "You must upload a copy of the current gas safety report";
    public static final String ELECTRICAL_INSTALLATION_REPORT_REQUIRED =
        "You must upload a copy of the current Electrical Installation Condition Report (EICR)";

    private static final List<String> DISALLOWED_FILE_TYPE_ERRORS =
        List.of(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);

    public List<String> validateDocuments(List<ListValue<Document>> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return List.of();
        }

        return disallowedFileErrors(documents.stream()
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(Document::getFilename));
    }

    /**
     * Validates one or more document uploads that appear together on one page. Each upload contributes its
     * {@code missingMessage} when it is required but empty, and the disallowed-file-type error is reported
     * at most once across all uploads. New uploads on a page are added by declaring another
     * {@link ConditionalDocumentUpload} rather than by adding more branching logic in the page callback.
     */
    public List<String> validateConditionalDocuments(List<ConditionalDocumentUpload> uploads) {
        List<String> errors = new ArrayList<>();
        boolean hasDisallowedFile = false;

        for (ConditionalDocumentUpload upload : uploads) {
            if (upload.required() && CollectionUtils.isEmpty(upload.documents())) {
                errors.add(upload.missingMessage());
            }
            hasDisallowedFile = hasDisallowedFile || !validateDocuments(upload.documents()).isEmpty();
        }

        if (hasDisallowedFile) {
            errors.addAll(DISALLOWED_FILE_TYPE_ERRORS);
        }
        return errors;
    }

    /**
     * A single document upload on a page: its uploaded {@code documents}, whether it is currently
     * {@code required} (for example because the caseworker confirmed they can provide it), and the
     * {@code missingMessage} to show when it is required but nothing has been uploaded.
     */
    public record ConditionalDocumentUpload(
        boolean required,
        List<ListValue<Document>> documents,
        String missingMessage) {
    }

    public List<String> validateAdditionalDocuments(List<ListValue<AdditionalDocument>> additionalDocuments) {
        if (CollectionUtils.isEmpty(additionalDocuments)) {
            return List.of();
        }

        return disallowedFileErrors(ListValueUtils.unwrapListItems(additionalDocuments).stream()
            .filter(Objects::nonNull)
            .map(AdditionalDocument::getDocument)
            .filter(Objects::nonNull)
            .map(Document::getFilename));
    }

    private List<String> disallowedFileErrors(Stream<String> filenames) {
        return filenames.anyMatch(this::isDisallowed) ? DISALLOWED_FILE_TYPE_ERRORS : List.of();
    }

    /**
     * Validates a required additional-document upload. Returns the given {@code requiredMessage} when no
     * document has been uploaded, otherwise applies the standard disallowed-file-type validation.
     */
    public List<String> validateRequiredAdditionalDocuments(
        List<ListValue<AdditionalDocument>> additionalDocuments, String requiredMessage) {
        if (CollectionUtils.isEmpty(additionalDocuments)) {
            return List.of(requiredMessage);
        }
        return validateAdditionalDocuments(additionalDocuments);
    }

    private boolean isDisallowed(String filename) {
        return !ALLOWED_FILE_EXTENSIONS.contains(getExtension(filename));
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase(Locale.UK);
    }
}
