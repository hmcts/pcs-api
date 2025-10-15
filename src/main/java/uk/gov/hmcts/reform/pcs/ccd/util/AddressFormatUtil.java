package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressFormatUtil {

    public static String getFormattedAddress(PCSCase caseData) {
        AddressUK propertyAddress = caseData.getPropertyAddress();
        return String.format(
            "%s<br>%s<br>%s",
            propertyAddress.getAddressLine1(),
            propertyAddress.getPostTown(),
            propertyAddress.getPostCode()
        );
    }

}
