package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Data
public class NameAndAddressForEviction {

    @CCD(label = "You can choose the defendants you want to evict on the next page")
    private VerticalYesNo correctNameAndAddress;

}
