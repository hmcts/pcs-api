package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Data
@Builder
@ComplexType(generate = true)
public class NameAndAddressForEviction {

    @CCD(label = "You can choose the defendants you want to evict on the next page")
    private VerticalYesNo correctNameAndAddress;

}
