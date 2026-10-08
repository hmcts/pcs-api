package uk.gov.hmcts.reform.pcs.testingsupport.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Outcome of writing and reading back a test blob in the SDP export container.
 * {@code error} is only set when {@code roundTrip} is false.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SdpExportRoundTripResult(String blobName, boolean roundTrip, String error) {

    public static SdpExportRoundTripResult success(String blobName) {
        return new SdpExportRoundTripResult(blobName, true, null);
    }

    public static SdpExportRoundTripResult failure(String blobName, String error) {
        return new SdpExportRoundTripResult(blobName, false, error);
    }
}
