package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.UUID;

@Service
@AllArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;

    public PartyEntity createAndLinkParty(PcsCaseEntity caseEntity,
                                          UUID userId, String forename,
                                          String surname, Boolean active) {
        PartyEntity party = PartyEntity.builder()
            .idamId(userId)
            .forename(forename)
            .surname(surname)
            .active(active)
            .pcsCase(caseEntity)
            .build();

        caseEntity.getParties().add(party);

        return party;
    }

    public PartyEntity saveParty(PartyEntity party) {
        return partyRepository.save(party);
    }

}
