package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NameAndAddressForEviction {

    @CCD(
        label = "Is this the correct name and address for the eviction?",
        hint = "You can choose the defendants you want to evict on the next page"
    )
    private VerticalYesNo correctNameAndAddress;

}
