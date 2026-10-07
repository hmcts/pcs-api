package uk.gov.hmcts.reform.pcs.document.model.claimform;

/**
 * Common address fields shared by the defendant and underlessee/mortgagee repeat rows, so the
 * builder can populate them with a single helper. The {@code @Data} on each row supplies these
 * setters.
 */
public interface ClaimFormAddressRow {
    void setAddressLine1(String addressLine1);

    void setAddressLine2(String addressLine2);

    void setAddressLine3(String addressLine3);

    void setPostTown(String postTown);

    void setCounty(String county);

    void setPostcode(String postcode);

    void setHasAddressLine2(boolean hasAddressLine2);

    void setHasAddressLine3(boolean hasAddressLine3);

    void setHasCounty(boolean hasCounty);
}
