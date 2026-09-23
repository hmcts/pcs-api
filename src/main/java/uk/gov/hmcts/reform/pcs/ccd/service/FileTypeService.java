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

    private static final String INVALID_FILE_TYPE_ERROR_MESSAGE = "%s contains a disallowed file type";

    public void validateNonMultiMediaFiles(List<ListValue<Document>> documents, List<String> errors) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        for (ListValue<Document> documentListValue : documents) {
            String fileName = documentListValue.getValue().getFilename();
            if (isNonMultiMediaFile(fileName)) {
                errors.add(String.format(INVALID_FILE_TYPE_ERROR_MESSAGE, fileName));
            }
        }
    }

    private boolean isNonMultiMediaFile(String fileName) {
        String fileExtension = StringUtils.substringAfterLast(fileName, ".");
        return EnumUtils.isValidEnumIgnoreCase(MultiMediaFileTypes.class, fileExtension);
    }

}
