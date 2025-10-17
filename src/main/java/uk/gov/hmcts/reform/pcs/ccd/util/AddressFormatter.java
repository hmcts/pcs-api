package uk.gov.hmcts.reform.pcs.ccd.util;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
public class AddressFormatter {

    public String getFormattedAddress(PCSCase caseData) {
        AddressUK propertyAddress = caseData.getPropertyAddress();
        return String.format(
            "%s<br>%s<br>%s",
            propertyAddress.getAddressLine1(),
            propertyAddress.getPostTown(),
            propertyAddress.getPostCode()
        );
    }

}
