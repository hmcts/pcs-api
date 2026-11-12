package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderStart;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppState.GEN_APP_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState.COUNTER_CLAIM_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper.toDomainParties;

/** The case as the order journeys show it: its property, and its main claim's parties. */
@Component
@AllArgsConstructor
class OrderCaseContext {

    private final AddressMapper addressMapper;
    private final PartyService partyService;
    private final CaseNameFormatter caseNameFormatter;

    public OrderStart.CaseContext of(PcsCaseEntity pcsCase) {
        Optional<ClaimEntity> claim = pcsCase.getClaims().stream().findFirst();
        return new OrderStart.CaseContext(
            pcsCase.getCaseReference(),
            addressMapper.toAddressUK(pcsCase.getPropertyAddress()),
            claim.map(this::buildCaseName).orElse(null),
            claim.map(c -> parties(c, PartyRole.CLAIMANT)).orElse(List.of()),
            claim.map(c -> parties(c, PartyRole.DEFENDANT)).orElse(List.of()),
            pcsCase.getCounterClaims().stream()
                .map(CounterClaimEntity::getStatus).anyMatch(COUNTER_CLAIM_ISSUED::equals),
            pcsCase.getGenApps().stream().map(GenAppEntity::getState).anyMatch(GEN_APP_ISSUED::equals)
        );
    }

    private List<OrderStart.Party> parties(ClaimEntity claim, PartyRole role) {
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .map(claimParty -> new OrderStart.Party(
                claimParty.getId().getPartyId().toString(), partyService.getPartyName(claimParty.getParty())))
            .toList();
    }

    private String buildCaseName(ClaimEntity mainClaim) {
        List<PartyEntity> claimants = PartyDisplayMapper.partiesByRole(mainClaim, PartyRole.CLAIMANT);
        List<PartyEntity> defendants = PartyDisplayMapper.partiesByRole(mainClaim, PartyRole.DEFENDANT);
        return caseNameFormatter.formatCaseName(toDomainParties(claimants), toDomainParties(defendants));
    }

}
