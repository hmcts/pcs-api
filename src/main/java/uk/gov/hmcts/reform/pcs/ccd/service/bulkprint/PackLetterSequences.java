package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.Comparator;
import java.util.List;

/**
 * Recipient numbers stamped onto defence and general-application letter codes so each copy is
 * {@code DEF-01}, {@code DEF-02}, ... or {@code GEN-01}, {@code GEN-02}, ... rather than one shared name.
 */
final class PackLetterSequences {

    private PackLetterSequences() {
    }

    static int defendantNumber(PcsCaseEntity pcsCase, PartyEntity recipient) {
        List<ClaimPartyEntity> defendants = parties(pcsCase, PartyRole.DEFENDANT);
        for (ClaimPartyEntity claimParty : defendants) {
            if (sameParty(claimParty, recipient) && claimParty.getRank() != null && claimParty.getRank() > 0) {
                return claimParty.getRank();
            }
        }
        return position(defendants, recipient);
    }

    /**
     * Defendants keep their case rank ({@code 1} is defendant 1). A claimant is numbered after every
     * defendant so a claimant copy does not reuse defendant 1's code.
     */
    static int genAppNumber(PcsCaseEntity pcsCase, PartyEntity recipient, PartyRole role) {
        if (role == PartyRole.DEFENDANT) {
            return defendantNumber(pcsCase, recipient);
        }
        List<ClaimPartyEntity> claimants = parties(pcsCase, PartyRole.CLAIMANT);
        int claimantNumber = 1;
        for (ClaimPartyEntity claimParty : claimants) {
            if (!sameParty(claimParty, recipient)) {
                continue;
            }
            claimantNumber = claimParty.getRank() != null && claimParty.getRank() > 0
                ? claimParty.getRank()
                : position(claimants, recipient);
            break;
        }
        return parties(pcsCase, PartyRole.DEFENDANT).size() + claimantNumber;
    }

    private static int position(List<ClaimPartyEntity> parties, PartyEntity recipient) {
        for (int index = 0; index < parties.size(); index++) {
            if (sameParty(parties.get(index), recipient)) {
                return index + 1;
            }
        }
        return 1;
    }

    private static boolean sameParty(ClaimPartyEntity claimParty, PartyEntity recipient) {
        return claimParty.getParty() != null
            && recipient.getId() != null
            && recipient.getId().equals(claimParty.getParty().getId());
    }

    private static List<ClaimPartyEntity> parties(PcsCaseEntity pcsCase, PartyRole role) {
        if (pcsCase.getClaims() == null || pcsCase.getClaims().isEmpty()
            || pcsCase.getClaims().getFirst().getClaimParties() == null) {
            return List.of();
        }
        return pcsCase.getClaims().getFirst().getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role && claimParty.getParty() != null)
            .sorted(Comparator.comparing(claimParty -> claimParty.getRank() == null
                ? Integer.MAX_VALUE : claimParty.getRank()))
            .toList();
    }
}
