package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CitizenClaimWhoIsClaiming implements HasLabel {

    MYSELF("Myself - I'm the claimant"),
    SOLICITOR("A client - I'm their solicitor"),
    LITIGATION_FRIEND("Someone else - I'm their litigation friend"),
    AGENT("Someone else - I'm their agent");

    private final String label;
}
