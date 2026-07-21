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
    MISSING_POSTAL_ADDRESS("MISSING_POSTAL_ADDRESS", "No postal address for party"),
    DEFENDANT_ACCESS_VALIDATOR("DEFENDANT_ACCESS_VALIDATOR",
                               "User is not linked as a defendant on this case"),
    DEFENDANT_PARTY_EXTRACTOR("DEFENDANT_PARTY_EXTRACTOR_01", "No claim found for this case"),
    DEFENDANT_PARTY_EXTRACTOR_NO_DEFENDANTS("DEFENDANT_PARTY_EXTRACTOR_02",
                                            "No defendants associated with this case"),
    LEGAL_REP_FOR_DEFENDANT_ACCESS("LEGAL_REP_FOR_DEFENDANT_ACCESS",
                                   "User is not linked as a defendant solicitor on this case"),
    LEGAL_REP_PARTY_SELECTION("LEGAL_REP_PARTY_SELECTION",
                              "User is not linked as a defendant on this case"),
    CASE_NOT_FOUND("CASE_NOT_FOUND","No case found with reference"),
    CLAIM_NOT_FOUND("CLAIM_NOT_FOUND","No claim found for case reference"),
    FEE_PAYMENT("FEE_PAYMENT_01", "No fee payment entity found"),
    FEE_PAYMENT_NOTIFICATION("FEE_PAYMENT_02", "Fee payment not found"),
    GEN_APP("GEN_APP", "Statement of truth must be accepted to create a gen app"),
    GEN_APP_NOT_FOUND("GEN_APP_NOT_FOUND", "Unable to find gen app"),

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
