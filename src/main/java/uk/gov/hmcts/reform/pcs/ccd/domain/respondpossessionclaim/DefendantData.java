package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantData {

    @CCD
    private DefendantContactDetails contactDetails;

    @CCD
    private DefendantResponses responses;
}
