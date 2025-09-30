package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum AdditionalDocumentType implements HasLabel {

    @JsonProperty("witnessStatement")
    WITNESS_STATEMENT("Witness Statement"),

    @JsonProperty("rentStatement")
    RENT_STATEMENT("Rent Statement"),

    @JsonProperty("tenancyAgreement")
    TENANCY_AGREEMENT("Tenancy Agreement"),

    @JsonProperty("letterFromClaimant")
    LETTER_FROM_CLAIMANT("Letter from Claimant"),

    @JsonProperty("statementOfService")
    STATEMENT_OF_SERVICE("Statement of Service"),

    @JsonProperty("videoEvidence")
    VIDEO_EVIDENCE("Video Evidence"),

    @JsonProperty("photographicEvidence")
    PHOTOGRAPHIC_EVIDENCE("Photographic Evidence");

    private final String label;
}
