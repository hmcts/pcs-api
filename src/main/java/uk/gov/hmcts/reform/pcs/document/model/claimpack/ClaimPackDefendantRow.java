package uk.gov.hmcts.reform.pcs.document.model.claimpack;

import lombok.Builder;
import lombok.Data;

/**
 * One row in the {@code defendants} repeating section of the claim pack template.
 *
 * <p>All display logic is pre-resolved by {@code ClaimPackPayloadBuilder} — the template just
 * renders the fields. {@code heading} is the rendered "Defendant N details" string,
 * {@code displayName} is "First Last" / orgName / "Persons unknown", and the address fields are
 * the defendant's own address OR the property address (fallback when defendant has none).</p>
 *
 * <p>{@code hasAddressLine2}/{@code hasAddressLine3}/{@code hasCounty} drive paragraph-removal
 * conditionals so null address lines don't render as blank rows.</p>
 */
@Data
@Builder
public class ClaimPackDefendantRow {

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
