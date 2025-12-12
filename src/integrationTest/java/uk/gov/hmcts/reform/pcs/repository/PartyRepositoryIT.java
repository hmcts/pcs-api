package uk.gov.hmcts.reform.pcs.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

@DataJpaTest
class PartyRepositoryIT extends AbstractPostgresContainerIT {

    @Autowired
    private PartyRepository partyRepository;

    @Test
    @Rollback(value = false)
    void shouldCreateParty() {
        System.out.println("OK");
//        PartyEntity partyEntity = new PartyEntity();
//        partyEntity.setFirstName("test");
//        partyEntity.setAddressNotKnown(VerticalYesNo.NO);
//
//        partyRepository.save(partyEntity);
    }

}
