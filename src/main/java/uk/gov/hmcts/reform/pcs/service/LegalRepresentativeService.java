package uk.gov.hmcts.reform.pcs.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalRepresentativeService {

    private final LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    /**
     * Gets a {@link uk.gov.hmcts.ccd.sdk.type.DynamicList} with the entity IDs
     * and party names for parties represented by a legal representative.
     * @param legalRepOrgId The organisation ID of the legal representative
     * @param caseReference The current case reference
     * @return An {@link Optional} containing a {@link DynamicList} of zero of more
     *     represented parties, or {@link Optional#empty()} if the IDAM ID does not
     *     correspond to a known legal rep in the PCS database
     */
    public Optional<DynamicList> getRepresentedPartiesDynamicList(String legalRepOrgId, long caseReference) {
        return legalRepresentativeOrganisationRepository.findByOrganisationIdAndCaseReference(legalRepOrgId,
                                                                                              caseReference)
            .map(
                legalRepresentativeOrganisationEntity -> {
                    List<PartyEntity> partyEntities = legalRepresentativeOrganisationEntity
                        .getPartyLegalRepresentativeOrganisationList()
                        .stream()
                        .map(PartyLegalRepresentativeOrganisationEntity::getParty)
                        .toList();
                    return createPartyNamesDynamicList(partyEntities);
                }
            );
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
