package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Flags;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartySupport {

    @CCD(
        label = "Support",
        retainHiddenValue = true
    )
    private Flags supportFlags;

}
