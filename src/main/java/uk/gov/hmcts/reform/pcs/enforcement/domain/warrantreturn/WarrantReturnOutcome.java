package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * The outcome of a final warrant return (HDPI-7347). Only a final return has one.
 */
@AllArgsConstructor
@Getter
public enum WarrantReturnOutcome implements HasLabel {

    EVICTION_SUCCESSFUL("Eviction successful"),
    EVICTION_UNSUCCESSFUL("Eviction unsuccessful"),
    EVICTION_CANCELLED("Eviction cancelled");

    private final String label;
}
