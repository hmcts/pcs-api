package uk.gov.hmcts.reform.pcs.enforcement.batchprint;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Why a case the bailiff chose is left out of a batch print.
 *
 * <p>A case that does not exist gets {@link #NOT_ALLOCATED} too, so that the answer does not tell a bailiff
 * which case references exist.
 */
@AllArgsConstructor
@Getter
public enum BatchPrintExclusion {

    INVALID_REFERENCE("This is not a valid case reference"),
    NOT_ALLOCATED("You are not the allocated bailiff on this case"),
    NO_LIVE_WARRANT("This case does not have a live warrant"),
    NO_DOCUMENTS("This case has no warrant, risk assessment or notice of eviction to print"),
    DOCUMENT_UNAVAILABLE("A document on this case could not be retrieved. Try again later");

    private final String reason;
}
