package uk.gov.hmcts.reform.pcs.ccd.service;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocument;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Service for validating uploaded documents against the disallowed multimedia file types.
 */
@Service
public class FileUploadValidationService {

    public static final Set<String> BLOCKED_MEDIA_EXTENSIONS = Set.of("mp3", "m4a", "mp4", "mpeg", "mpg");
    public static final String DISALLOWED_FILE_TYPE_ERROR = "Your upload contains a disallowed file type";
    public static final String ALLOWED_FILE_TYPE_GUIDANCE =
        "The selected file must be a DOC/DOT/DOCX/DOTX, XLS/XLT/XLA/XLSX/XLTX/XLSB, "
        + "PPT/POT/PPS/PPA/PPTX/POTX/PPSX, PDF, TXT/RTF/CSV, JPG/JPEG, PNG, BMP, TIF/TIFF.";

    private static final List<String> DISALLOWED_FILE_TYPE_ERRORS =
        List.of(DISALLOWED_FILE_TYPE_ERROR, ALLOWED_FILE_TYPE_GUIDANCE);

    public List<String> validateDocuments(List<ListValue<Document>> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return List.of();
        }

        boolean hasBlockedFile = documents.stream()
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(Document::getFilename)
            .anyMatch(this::isBlocked);

        return hasBlockedFile ? DISALLOWED_FILE_TYPE_ERRORS : List.of();
    }

    /**
     * Validates several document lists together, returning the disallowed-file-type error at most once
     * even when more than one list contains a blocked file.
     */
    @SafeVarargs
    public final List<String> validateDocumentGroups(List<ListValue<Document>>... documentGroups) {
        for (List<ListValue<Document>> documents : documentGroups) {
            if (!validateDocuments(documents).isEmpty()) {
                return DISALLOWED_FILE_TYPE_ERRORS;
            }
        }
        return List.of();
    }

    public List<String> validateAdditionalDocuments(List<ListValue<AdditionalDocument>> additionalDocuments) {
        if (CollectionUtils.isEmpty(additionalDocuments)) {
            return List.of();
        }

        boolean hasBlockedFile = ListValueUtils.unwrapListItems(additionalDocuments).stream()
            .filter(Objects::nonNull)
            .map(AdditionalDocument::getDocument)
            .filter(Objects::nonNull)
            .map(Document::getFilename)
            .anyMatch(this::isBlocked);

        return hasBlockedFile ? DISALLOWED_FILE_TYPE_ERRORS : List.of();
    }

    private boolean isBlocked(String filename) {
        return BLOCKED_MEDIA_EXTENSIONS.contains(getExtension(filename));
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
