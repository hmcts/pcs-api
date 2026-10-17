package uk.gov.hmcts.reform.pcs.enforcement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The "Eviction details" the bailiff sees when scheduling or rescheduling the eviction (HDPI-7344): the claimant
 * type, claimant names and defendant names of the claim. Defendants whose names are not known are shown once,
 * as "Person(s) unknown", however many there are.
 */
@Service
@RequiredArgsConstructor
public class EvictionDetailsService {

    public static final String PERSONS_UNKNOWN = "Person(s) unknown";

    private final PcsCaseService pcsCaseService;

    public record EvictionDetails(String claimantType, List<String> claimantNames, List<String> defendantNames) {
    }

    @Transactional(readOnly = true)
    public EvictionDetails getEvictionDetails(long caseReference) {
        return getEvictionDetails(pcsCaseService.loadCase(caseReference));
    }

    public EvictionDetails getEvictionDetails(PcsCaseEntity pcsCaseEntity) {
        Optional<ClaimEntity> claim = Optional.ofNullable(pcsCaseEntity.getClaims())
            .flatMap(claims -> claims.stream().findFirst());
        List<ClaimPartyEntity> claimParties = claim.map(ClaimEntity::getClaimParties).orElse(List.of());

        String claimantType = claim.map(ClaimEntity::getClaimantType)
            .or(() -> Optional.ofNullable(pcsCaseEntity.getClaimantType()))
            .map(ClaimantType::getLabel)
            .orElse(null);
        List<String> claimantNames = partiesWithRole(claimParties, PartyRole.CLAIMANT)
            .map(EvictionDetailsService::claimantName)
            .filter(Objects::nonNull)
            .toList();
        List<String> defendantNames = defendantNames(partiesWithRole(claimParties, PartyRole.DEFENDANT));
        return new EvictionDetails(claimantType, claimantNames, defendantNames);
    }

    private static Stream<PartyEntity> partiesWithRole(List<ClaimPartyEntity> claimParties, PartyRole role) {
        return claimParties.stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .filter(claimParty -> claimParty.getParty() != null && !claimParty.getParty().isRemoved())
            .sorted(Comparator.comparing(ClaimPartyEntity::getRank, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(ClaimPartyEntity::getParty);
    }

    private static String claimantName(PartyEntity party) {
        return party.getOrgName() != null ? party.getOrgName() : fullName(party);
    }

    /**
     * Every defendant's name, with "Person(s) unknown" shown once however many defendants have no known name.
     * Only that placeholder is deduplicated: two defendants can share a name, such as joint tenants or a
     * father and son, and each is a person the bailiff has to evict. The "Record warrant return" screen's
     * table applies the same rule.
     */
    private static List<String> defendantNames(Stream<PartyEntity> defendants) {
        List<String> names = new ArrayList<>();
        boolean anyUnknown = false;
        for (PartyEntity defendant : defendants.toList()) {
            String name = knownName(defendant);
            if (name == null) {
                if (!anyUnknown) {
                    names.add(PERSONS_UNKNOWN);
                }
                anyUnknown = true;
            } else {
                names.add(name);
            }
        }
        return names;
    }

    private static String knownName(PartyEntity party) {
        return party.getNameKnown() == VerticalYesNo.YES ? fullName(party) : null;
    }

    private static String fullName(PartyEntity party) {
        String name = Stream.of(party.getFirstName(), party.getLastName())
            .filter(part -> part != null && !part.isBlank())
            .collect(Collectors.joining(" "));
        return name.isEmpty() ? null : name;
    }
}
