package uk.gov.hmcts.reform.pcs.document.model.claimform;

import lombok.Builder;
import lombok.Data;

/**
 * Six-line address rendered into the claim form template.
 * Mirrors {@link uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity} minus the id.
 * Fields are individually addressable from the Docmosis template ({@code <<address.line1>>} etc.),
 * which is why we don't reuse the flattened {@link uk.gov.hmcts.reform.pcs.document.model.Party}
 * single-string address.
 */
@Data
@Builder
public class ClaimFormAddress {

    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String postTown;
    private String county;
    private String postcode;
    private String country;

}
