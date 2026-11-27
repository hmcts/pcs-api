package uk.gov.hmcts.reform.pcs.ccd.service.form;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartyDisplayMapperTest {

    @Test
    void shouldMapPartyEntitiesToDomainParties() {
        // Given
        PartyEntity partyEntity1 = PartyEntity.builder()
            .orgName("Some org name")
            .nameKnown(VerticalYesNo.NO)
            .build();

        PartyEntity partyEntity2 = PartyEntity.builder()
            .firstName("Danny")
            .lastName("Defendant")
            .nameKnown(VerticalYesNo.YES)
            .build();

        List<PartyEntity> partyEntities = List.of(partyEntity1, partyEntity2);

        // When
        List<Party> domainParties = PartyDisplayMapper.toDomainParties(partyEntities);

        // Then
        assertThat(domainParties)
            .containsExactly(
                Party.builder()
                    .orgName("Some org name")
                    .nameKnown(VerticalYesNo.NO)
                    .build(),
                Party.builder().firstName("Danny")
                    .lastName("Defendant")
                    .nameKnown(VerticalYesNo.YES)
                    .build()
            );
    }

}

