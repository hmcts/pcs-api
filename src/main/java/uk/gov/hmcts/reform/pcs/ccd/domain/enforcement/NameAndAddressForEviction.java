package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

public class NameAndAddressForEviction {

    @CCD(label = "You can choose the defendants you want to evict on the next page")
    private VerticalYesNo correctNameAndAddress;

}
