package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @InjectMocks
    private PartyService partyService;

    @Test
    void shouldCreateAndLinkParty() {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        UUID userId = UUID.randomUUID();
        String forename = "Alice";
        String surname = "Smith";
        Boolean active = true;

        PartyEntity party = partyService.createAndLinkParty(caseEntity, userId, forename, surname, active);

        assertThat(party.getIdamId()).isEqualTo(userId);
        assertThat(party.getForename()).isEqualTo(forename);
        assertThat(party.getSurname()).isEqualTo(surname);
        assertThat(party.getActive()).isEqualTo(active);
        assertThat(party.getPcsCase()).isSameAs(caseEntity);
        assertThat(caseEntity.getParties().size()).isEqualTo(1);
        assertThat(caseEntity.getParties().iterator().next()).isSameAs(party);
    }

    @Test
    void shouldSaveParty() {
        PartyEntity party = new PartyEntity();
        when(partyRepository.save(party)).thenReturn(party);

        PartyEntity result = partyService.saveParty(party);

        verify(partyRepository, times(1)).save(party);
        assertThat(result).isSameAs(party);
    }

}
