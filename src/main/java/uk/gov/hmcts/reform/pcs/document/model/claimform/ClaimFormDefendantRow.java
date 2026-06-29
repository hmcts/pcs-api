package uk.gov.hmcts.reform.pcs.document.model.claimform;

import lombok.Builder;
import lombok.Data;

/**
 * One row in the {@code defendants} repeating section of the claim form template.
 *
 * <p>{@code ClaimFormPayloadBuilder} resolves every field, so the template just renders them.
 * {@code heading} is the "Defendant N details" string, {@code displayName} is "First Last", an
 * org name, or "Persons unknown", and the address fields are the defendant's own address, or the
 * property address when the defendant has none.</p>
 *
 * <p>{@code hasAddressLine2}, {@code hasAddressLine3} and {@code hasCounty} drive paragraph
 * removal so empty address lines don't render as blank rows.</p>
 */
@Data
@Builder
public class ClaimFormDefendantRow implements ClaimFormAddressRow {

    private int defendantNumber;
    private String heading;
    private String displayName;

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
