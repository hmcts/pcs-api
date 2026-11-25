package uk.gov.hmcts.reform.pcs.feesandpay.model;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClaimIssueFeeResponse(
    @Schema(description = "Fee amount in pounds with two decimal places", example = "404.00")
    String amount,
    @Schema(description = "Fees register code", example = "FEE0412")
    String code,
    @Schema(description = "Fees register version of the fee", example = "4")
    Integer version
) {
}
