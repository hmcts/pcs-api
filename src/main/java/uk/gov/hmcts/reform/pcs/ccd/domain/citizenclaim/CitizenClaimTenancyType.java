package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;

@AllArgsConstructor
@Getter
public enum CitizenClaimTenancyType implements HasLabel {

    ASSURED("Assured or assured periodic tenancy", CombinedLicenceType.ASSURED_TENANCY),
    EXCLUDED("Excluded tenancy or licence", CombinedLicenceType.OTHER),
    REGULATED("Regulated tenancy", CombinedLicenceType.OTHER),
    OTHER("Other", CombinedLicenceType.OTHER);

    private final String label;
    private final CombinedLicenceType combinedLicenceType;
}
