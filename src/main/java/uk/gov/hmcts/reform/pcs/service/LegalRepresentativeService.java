package uk.gov.hmcts.reform.pcs.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalRepresentativeService {

    private final PartyRepository partyRepository;

    /**
     * Gets a {@link uk.gov.hmcts.ccd.sdk.type.DynamicList} with the entity IDs
     * and party names for parties represented by an organisation.
     * @param orgId The organisation ID of the legal representative
     * @param caseReference The current case reference
     * @return A {@link DynamicList} of zero of more
     *     represented parties if the organisation ID does not
     *     correspond to a known organisation in the PCS database
     */
    public DynamicList getRepresentedPartiesDynamicList(String orgId, long caseReference) {
        List<PartyEntity> parties = partyRepository.findAllPartiesByOrganisationIdAndCaseReference(
            orgId,
            caseReference
        );

        return createPartyNamesDynamicList(parties);
    }

    private DynamicList createPartyNamesDynamicList(List<PartyEntity> partyEntities) {
        List<DynamicListElement> listItems = partyEntities.stream()
            .map(partyEntity -> DynamicListElement.builder()
                .code(partyEntity.getId())
                .label(buildPartyName(partyEntity))
                .build())
            .toList();

        return DynamicList.builder()
            .listItems(listItems)
            .build();
    }

    private String buildPartyName(PartyEntity partyEntity) {
        return partyEntity.getFirstName() + " " + partyEntity.getLastName();
    }

}
