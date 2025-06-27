package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlFlags {

    @CCD
    private YesOrNo draftExists;

}
