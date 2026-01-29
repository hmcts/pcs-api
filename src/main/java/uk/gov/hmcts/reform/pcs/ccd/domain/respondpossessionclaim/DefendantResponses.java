package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;

import java.time.LocalDate;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefendantResponses {

    @CCD
    private YesNoNotSure tenancyTypeCorrect;

    @CCD
    private YesNoNotSure tenancyStartDateCorrect;

    @CCD
    private YesNoNotSure oweRentArrears;

    @CCD
    private YesNoNotSure noticeReceived;

    @CCD
    private LocalDate noticeReceivedDate;
}
