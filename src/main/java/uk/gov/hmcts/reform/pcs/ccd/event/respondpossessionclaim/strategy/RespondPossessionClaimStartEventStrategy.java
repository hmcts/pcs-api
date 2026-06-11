package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.strategy;

import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.List;

public interface RespondPossessionClaimStartEventStrategy {

    boolean supports(List<String> roles);

    PCSCase loadDraft(long caseReference, PCSCase pcsCase);
}
