package uk.gov.hmcts.reform.pcs.ccd.service.document;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;

public interface DocumentFolderSelection {

    CaseFileCategory getSelectedFolder();

    void setPropertyAddressSummary(String propertyAddressSummary);

    void setEmptyForCategory(CaseFileCategory category, YesOrNo empty);

    void setSelectedFolderId(String id);

    void setSelectedFolderLabel(String label);

    void setSelectedDocumentId(String id);

    void setSelectedDocumentFileName(String fileName);
}
