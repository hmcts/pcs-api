package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReasonableAdjustments {

    @CCD(access = {CitizenAccess.class})
    private String reasonableAdjustmentsRequired;
}
