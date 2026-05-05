package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class PartiesView implements ViewComponent {

    private final ModelMapper modelMapper;

    @Override
    public void setCaseFields(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        pcsCase.setParties(mapAndWrapParties(pcsCaseEntity.getParties()));

        Map<PartyRole, List<ListValue<Party>>> partyMap = getPartyMap(pcsCaseEntity);
        pcsCase.setAllClaimants(partyMap.get(PartyRole.CLAIMANT));
        pcsCase.setAllDefendants(partyMap.get(PartyRole.DEFENDANT));
        pcsCase.setAllUnderlesseeOrMortgagees(partyMap.get(PartyRole.UNDERLESSEE_OR_MORTGAGEE));
    }

    private List<ListValue<Party>> mapAndWrapParties(Set<PartyEntity> partyEntities) {
        return partyEntities.stream()
            .map(entity -> modelMapper.map(entity, Party.class))
            .collect(Collectors.collectingAndThen(Collectors.toList(), ListValueUtils::wrapListItems));
    }

    private Map<PartyRole, List<ListValue<Party>>> getPartyMap(PcsCaseEntity pcsCaseEntity) {
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();

        if (claims.isEmpty()) {
            return Map.of();
        }

        ClaimEntity mainClaim = claims.getFirst();
        return mainClaim.getClaimParties().stream()
            .collect(Collectors.groupingBy(
                ClaimPartyEntity::getRole,
                Collectors.mapping(this::getPartyListValue, Collectors.toList())
            ));
    }

    private ListValue<Party> getPartyListValue(ClaimPartyEntity claimPartyEntity) {
        Party party = modelMapper.map(claimPartyEntity.getParty(), Party.class);

        return ListValue.<Party>builder()
            .id(claimPartyEntity.getId().getPartyId().toString())
            .value(party)
            .build();
    }

}
