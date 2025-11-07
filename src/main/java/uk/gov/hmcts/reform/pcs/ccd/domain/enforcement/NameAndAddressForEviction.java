package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameAndAddressForEviction {

    @CCD(label = "You can choose the defendants you want to evict on the next page")
    private VerticalYesNo correctNameAndAddress;

}
