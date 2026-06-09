package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class RespondPossessionClaimSubmitResponse {

    private final CounterClaimSubmitResponse counterClaim;
}
