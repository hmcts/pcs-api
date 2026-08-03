package uk.gov.hmcts.reform.pcs.document.model.claimform;

import lombok.Builder;
import lombok.Data;

/**
 * One row in the {@code underlessees} repeating section of the claim form template.
 *
 * <p>Mirrors {@link ClaimFormDefendantRow} in shape, but address semantics differ: per the
 * mapping spec, an underlessee's address "will either be provided or it will be
 * 'Address unknown'". When the address is missing, render the single line
 * "Address unknown" instead of the full 6-line block.</p>
 *
 * <p>{@code addressKnown} and {@code addressUnknown} are complementary booleans because
 * Docmosis compact section syntax can't negate.</p>
 */
@Data
@Builder
public class ClaimFormUnderlesseeRow implements ClaimFormAddressRow {

    private int underlesseeNumber;
    private String heading;
    private String displayName;

    private boolean addressKnown;
    private boolean addressUnknown;

    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String postTown;
    private String county;
    private String postcode;

    private boolean hasAddressLine2;
    private boolean hasAddressLine3;
    private boolean hasCounty;

}
