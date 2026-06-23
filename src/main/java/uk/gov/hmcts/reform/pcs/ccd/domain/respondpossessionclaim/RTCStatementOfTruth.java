package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

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
public class RTCStatementOfTruth {

    @CCD
    private VerticalYesNo accepted;

    @CCD(max = 100)
    private String fullName;

}