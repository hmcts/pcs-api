package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.caseworkerUpdateCase;

@Component
@Slf4j
@AllArgsConstructor
public class CaseworkerUpdateCase implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseRepository pcsCaseRepository;

    @Override
    public void configureDecentralised(final DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(caseworkerUpdateCase.name(), this::submit)
            .forAllStates()
            .name("Update case (caseworker)")
            .description("Update a possession case, for caseworkers")
            .grant(Permission.CRU, UserRole.PCS_CASE_WORKER);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Caseworker updated case {}", caseReference);

        String claimantEmail = pcsCase.getClaimantContactEmail();
        log.info("Email = " + claimantEmail);

        updateClaimantEmail(caseReference, claimantEmail);

        return SubmitResponse.defaultResponse();
    }


    private void updateClaimantEmail(long caseReference, String newEmail) {
        PcsCaseEntity pcsCaseEntity = loadCaseData(caseReference);

        Set<ClaimEntity> claims = pcsCaseEntity.getClaims();

        if (claims.isEmpty()) {
            log.warn("No main claim found");
            return;
        }

        ClaimEntity mainClaim = claims.iterator().next();
        mainClaim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT)
            .map(ClaimPartyEntity::getParty)
            .findFirst()
            .ifPresent(party -> party.setContactEmail(newEmail));

    }

    private PcsCaseEntity loadCaseData(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

}
