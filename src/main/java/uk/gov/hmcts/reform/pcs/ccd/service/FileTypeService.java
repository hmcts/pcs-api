package uk.gov.hmcts.reform.pcs.ccd.service;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.model.MultiMediaFileTypes;

import java.util.List;

@Service
public class FileTypeService {

    private static final String INVALID_FILE_TYPE_ERROR_MESSAGE = "The selected file must be a DOC/DOT/DOCX/DOTX, "
        + "XLS/XLT/XLA/XLSX/XLTX/XLSB, PPT/POT/PPS/PPA/PPTX/POTX/PPSX, PDF, TXT/RTF/CSV, JPG/JPEG, PNG, BMP, TIF/TIFF.";

    public void validateNonMultiMediaFiles(List<ListValue<Document>> documents, List<String> errors) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        boolean hasInvalidFile = documents.stream()
            .anyMatch(documentListValue -> isNonMultiMediaFile(documentListValue.getValue().getFilename()));

        if (hasInvalidFile) {
            errors.add(INVALID_FILE_TYPE_ERROR_MESSAGE);
        }
    }

    private boolean isNonMultiMediaFile(String fileName) {
        String fileExtension = StringUtils.substringAfterLast(fileName, ".");
        return EnumUtils.isValidEnumIgnoreCase(MultiMediaFileTypes.class, fileExtension);
    }

}
