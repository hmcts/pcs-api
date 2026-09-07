package uk.gov.hmcts.reform.pcs.config;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PartyMapperTest {

    private final ModelMapper modelMapper = new MapperConfig().modelMapper();

    @Test
    void shouldMapPartyEntityIdOntoTheProjectedParty() {
        UUID partyId = UUID.randomUUID();

        Party party = modelMapper.map(
            PartyEntity.builder().id(partyId).firstName("Danny").lastName("Defendant").build(),
            Party.class);

        assertThat(party.getId()).isEqualTo(partyId.toString());
    }

    @Test
    void shouldLeaveTheProjectedIdUnsetWhenTheEntityHasNoId() {
        Party party = modelMapper.map(
            PartyEntity.builder().firstName("Danny").lastName("Defendant").build(), Party.class);

        assertThat(party.getId()).isNull();
    }
}
