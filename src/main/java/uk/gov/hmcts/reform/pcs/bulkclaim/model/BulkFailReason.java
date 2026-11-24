package uk.gov.hmcts.reform.pcs.bulkclaim.model;

import lombok.Getter;

/**
 * Status codes and fail reasons reported in a bulk claim response. {@code statusCode} and
 * {@code statusReason} populate the response StatusType; {@code failReasonCode} and {@code reason}
 * populate its optional FailReasonCode and FailReason.
 */
@Getter
public enum BulkFailReason {

    BULK_SUCCESS(1, "All claims processed successfully", null, null),

    PBA_NOT_VALID(2, BulkFailReason.BULK_FAILED_TEXT, 1,
                  "The payment account is not valid for this organisation"),
    ORGANISATION_NOT_USABLE(2, BulkFailReason.BULK_FAILED_TEXT, 2,
                            "The organisation is not able to submit bulk claims"),
    SCHEMA_INVALID(2, BulkFailReason.BULK_FAILED_TEXT, 3,
                   "The request sent does not conform to the XML schema"),
    CLAIMS_FAILED(2, BulkFailReason.BULK_FAILED_TEXT, 4,
                  "One or more claims failed to process"),

    CLAIM_SUCCESS(1, "This claim was processed successfully", null, null),
    CLAIM_PENDING(3, "This claim is still being processed", null, null),

    CLAIM_REF_GENERATION_FAILED(2, BulkFailReason.CLAIM_FAILED_TEXT, 5,
                                "Failed to generate a claim reference"),
    SAVE_FAILED(2, BulkFailReason.CLAIM_FAILED_TEXT, 6,
                "Failed to save claim. Please try again"),
    NO_COURT_FOR_POSTCODE(2, BulkFailReason.CLAIM_FAILED_TEXT, 7,
                          "No court was found for the property postcode"),
    CODED_PARTY_NOT_FOUND(2, BulkFailReason.CLAIM_FAILED_TEXT, 8,
                          "The claimant or legal representative could not be found"),
    VALIDATION_FAILED(2, BulkFailReason.CLAIM_FAILED_TEXT, 9,
                      "The claim failed validation. See the validation errors"),
    CLAIMANT_TYPE_NOT_ELIGIBLE(2, BulkFailReason.CLAIM_FAILED_TEXT, 10,
                               "The claimant type is not eligible to make this claim"),
    PROPERTY_NOT_ELIGIBLE(2, BulkFailReason.CLAIM_FAILED_TEXT, 11,
                          "The property is not eligible for a claim in this service"),
    LEGISLATIVE_COUNTRY_REQUIRED(2, BulkFailReason.CLAIM_FAILED_TEXT, 12,
                                 "The legislative country is required for this property"),
    MULTIPLE_COURTS_FOR_POSTCODE(2, BulkFailReason.CLAIM_FAILED_TEXT, 13,
                                 "More than one court matches the property postcode"),
    IDENTIFIER_REUSED(2, BulkFailReason.CLAIM_FAILED_TEXT, 14,
                      "The customer unique identifier has already been used for a different claim"),
    CLAIM_KIND_NOT_ENABLED(2, BulkFailReason.CLAIM_FAILED_TEXT, 15,
                           "This kind of bulk claim is not enabled"),
    PBA_OVERRIDE_NOT_VALID(2, BulkFailReason.CLAIM_FAILED_TEXT, 16,
                           "The payment account supplied for this claim is not valid for this organisation"),
    CLAIM_NOT_FOUND(2, "This claim could not be found", 17,
                    "No claim was found for this customer unique identifier");

    public static final String BULK_FAILED_TEXT =
        "At least one claim failed to process completely. See individual claim responses for reason";
    public static final String CLAIM_FAILED_TEXT = "This claim failed to be processed";

    private final int statusCode;
    private final String statusReason;
    private final Integer failReasonCode;
    private final String reason;

    BulkFailReason(int statusCode, String statusReason, Integer failReasonCode, String reason) {
        this.statusCode = statusCode;
        this.statusReason = statusReason;
        this.failReasonCode = failReasonCode;
        this.reason = reason;
    }
}
