package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganisationEntityTest {

    private OrganisationEntity underTest;

    @BeforeEach
    void setUp() {
        underTest = new OrganisationEntity();
    }

    @Test
    void addParty_ShouldNotAddPartyIfAlreadyPresent() {
        // given
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder()
            .id(partyId)
            .build();

        underTest.addParty(party);

        // when
        underTest.addParty(party);

        // verify
        assertThat(underTest.getClaimPartyOrganisationList().size()).isEqualTo(1);

        ClaimPartyOrganisationEntity partyLegalRepresentativeOrganisation =
            underTest.getClaimPartyOrganisationList().getFirst();

        assertThat(partyLegalRepresentativeOrganisation.getParty().getId()).isEqualTo(partyId);
    }

    @Test
    void addParty_ShouldAddPartyIfNotAlreadyPresent() {
        // given
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder()
            .id(partyId)
            .build();

        underTest.addParty(party);

        UUID partyId2 = UUID.randomUUID();
        PartyEntity party2 = PartyEntity.builder()
            .id(partyId2)
            .build();

        // when
        underTest.addParty(party2);

        // verify
        assertThat(underTest.getClaimPartyOrganisationList().size()).isEqualTo(2);

        ClaimPartyOrganisationEntity partyLegalRepresentativeOrganisation =
            underTest.getClaimPartyOrganisationList().getFirst();

        assertThat(partyLegalRepresentativeOrganisation.getParty().getId()).isEqualTo(partyId);


        ClaimPartyOrganisationEntity partyLegalRepresentativeOrganisation2 =
            underTest.getClaimPartyOrganisationList().get(1);

        assertThat(partyLegalRepresentativeOrganisation2.getParty().getId()).isEqualTo(partyId2);
        assertThat(partyLegalRepresentativeOrganisation2.getOrganisation()).isEqualTo(underTest);
        assertThat(partyLegalRepresentativeOrganisation2.getActive()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void addLegalRepresentativeOrganisationContactDetails_WithContactDetails_SetsReference() {
        // given
        ClaimPartyContactDetailsEntity contactDetails =
            new ClaimPartyContactDetailsEntity();

        // when
        underTest.addClaimPartyContactDetails(contactDetails);

        // then
        assertThat(contactDetails.getOrganisation()).isEqualTo(underTest);
        assertThat(underTest.getClaimPartyContactDetails().getFirst()).isEqualTo(contactDetails);
    }

    @Test
    void addLegalRepresentativeOrganisationContactDetails_WithNullContactDetails_DoesNotSetReference() {
        // when
        underTest.addClaimPartyContactDetails(null);

        // then
        assertTrue(underTest.getClaimPartyContactDetails().isEmpty());
    }

}
