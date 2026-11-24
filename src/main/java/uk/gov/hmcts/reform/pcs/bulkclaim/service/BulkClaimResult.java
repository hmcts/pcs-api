package uk.gov.hmcts.reform.pcs.bulkclaim.service;

import java.util.Arrays;
import java.util.Objects;

/**
 * Transport-neutral outcome of a bulk claim request. The XML is the response document; it is absent only
 * for {@link Outcome#TOO_MANY_CLAIMS}, which the transport reports as a refusal of the whole request.
 */
public record BulkClaimResult(Outcome outcome, byte[] xml) {

    public enum Outcome {
        /** Processed; the response carries the overall and per-claim statuses. */
        OK,
        /** The payload does not conform to the schema; the response carries the errors. */
        SCHEMA_INVALID,
        /** The caller's organisation cannot submit bulk claims. */
        ORGANISATION_NOT_USABLE,
        /** More claims or identifiers than the configured maximum. */
        TOO_MANY_CLAIMS,
        /** A single status request for a claim the organisation has not submitted. */
        CLAIM_NOT_FOUND
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BulkClaimResult that && outcome == that.outcome && Arrays.equals(xml, that.xml);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(outcome) + Arrays.hashCode(xml);
    }

    @Override
    public String toString() {
        return "BulkClaimResult[outcome=" + outcome + ", xml=" + (xml == null ? "null" : xml.length + " bytes") + "]";
    }

    static BulkClaimResult of(Outcome outcome, byte[] xml) {
        return new BulkClaimResult(outcome, xml);
    }

    static BulkClaimResult tooManyClaims() {
        return new BulkClaimResult(Outcome.TOO_MANY_CLAIMS, null);
    }
}
