package uk.gov.hmcts.reform.pcs.exception;

public enum ErrorCode {

    DOC_ASSEMBLY_NO_URL_RETURNED("DOC_ASSEMBLY_1",
                                 "No document URL returned from Doc Assembly service"),
    DOC_GENERATION_FAILED("DOC_GENERATION_1",
                          "Document generation failed"),
    DOC_GENERATION_UNEXPECTED_ERROR("DOC_GENERATION_2",
                          "Unexpected error occurred during document generation"),


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
