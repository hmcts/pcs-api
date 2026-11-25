package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CitizenClaimNoticeType implements HasLabel {

    NOTICE_SEEKING_POSSESSION("Notice seeking possession"),
    NOTICE_TO_QUIT("Notice to quit"),
    OTHER("Other");

    private final String label;
}
