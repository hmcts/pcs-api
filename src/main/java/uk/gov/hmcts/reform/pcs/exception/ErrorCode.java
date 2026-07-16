package uk.gov.hmcts.reform.pcs.exception;

public enum ErrorCode {

    DOC_ASSEMBLY_NO_URL_RETURNED("DOC_ASSEMBLY_1",
                                 "No document URL returned from Doc Assembly service"),
    DOC_GENERATION_FAILED("DOC_GENERATION_1",
                          "Document generation failed"),
    DOC_GENERATION_UNEXPECTED_ERROR("DOC_GENERATION_2",
                          "Unexpected error occurred during document generation"),
    ACCESS_CODE_ALREADY_IN_USE("ACCESS_CODE",
                               "This access code is already linked to a user."),
    BULK_PRINT_MERGE_ERROR("BULK_PRINT_MERGE", "Failed to merge bulk-print PDFs"),
    META_DATA_FOR_DOCUMENT_ERROR("META_DATA_ERROR",
                                 "Failed to retrieve document metadata from CDAM"),


    TEST_CASE_SUPPORT("", "");

    private final String internalCode;
    private final String safeDescription;

    ErrorCode(String internalCode, String safeDescription) {
        this.internalCode = internalCode;
        this.safeDescription = safeDescription;
    }

    public String internalCode() {
        return internalCode;
    }

    public String safeDescription() {
        return safeDescription;
    }

}
