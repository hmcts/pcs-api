package uk.gov.hmcts.reform.pcs.ccd.service.form;

import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.Comparator;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.service.form.FormFieldFormatter.isPopulated;

/**
 * Party helpers shared by the claim-form and defence-form builders: name joining, the
 * {@link PartyEntity} to {@link Party} mapping used for case-name formatting, and rank-ordered
 * selection of a claim's parties by role.
 */
public final class PartyDisplayMapper {

    private PartyDisplayMapper() {
    }

    public static String joinName(String firstName, String lastName) {
        boolean hasFirst = isPopulated(firstName);
        boolean hasLast = isPopulated(lastName);
        return (hasFirst ? firstName : "") + (hasFirst && hasLast ? " " : "") + (hasLast ? lastName : "");
    }

    public static Party toDomainParty(PartyEntity party) {
        return Party.builder()
            .firstName(party.getFirstName())
            .lastName(party.getLastName())
            .orgName(party.getOrgName())
            .nameKnown(party.getNameKnown())
            .build();
    }

    public static List<PartyEntity> partiesByRole(ClaimEntity claim, PartyRole role) {
        if (claim.getClaimParties() == null) {
            return List.of();
        }
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .sorted(Comparator.comparingInt(ClaimPartyEntity::getRank))
            .map(ClaimPartyEntity::getParty)
            .toList();
    }
}
