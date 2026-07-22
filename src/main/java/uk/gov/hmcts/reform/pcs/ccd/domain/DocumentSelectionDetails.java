package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

public interface DocumentSelectionDetails {

    CaseFileCategory getSelectedFolder();

    void setPropertyAddressSummary(String propertyAddressSummary);

    void setEmptyForCategory(CaseFileCategory category, YesOrNo empty);

    void setSelectedFolderId(String id);

    void setSelectedFolderLabel(String label);

    void setSelectedDocumentId(String id);

    void setSelectedDocumentFileName(String fileName);
}
