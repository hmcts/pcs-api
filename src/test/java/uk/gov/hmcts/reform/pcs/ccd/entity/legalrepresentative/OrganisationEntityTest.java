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
        assertThat(partyLegalRepresentativeOrganisation.getActive()).isEqualTo(YesOrNo.YES);
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
    void addParty_ShouldRelinkPartyWhenExistingLinkIsInactive() {
        // given - the party was previously represented by this organisation, then moved away
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder()
            .id(partyId)
            .build();

        underTest.addParty(party);
        underTest.getClaimPartyOrganisationList().getFirst().setActive(YesOrNo.NO);

        // when - a notice of change brings the party back to this organisation
        underTest.addParty(party);

        // then - a fresh active link is created alongside the historic inactive one
        assertThat(underTest.getClaimPartyOrganisationList()).hasSize(2);
        assertThat(underTest.getClaimPartyOrganisationList().getFirst().getActive()).isEqualTo(YesOrNo.NO);

        ClaimPartyOrganisationEntity relinked = underTest.getClaimPartyOrganisationList().get(1);
        assertThat(relinked.getParty().getId()).isEqualTo(partyId);
        assertThat(relinked.getActive()).isEqualTo(YesOrNo.YES);
        assertThat(relinked.getOrganisation()).isEqualTo(underTest);
    }

    @Test
    void addParty_ShouldNotAddPartyWhenActiveLinkMatchesByIdValue() {
        // given - same party id carried by distinct entity instances (e.g. separate loads)
        String partyId = "11111111-2222-3333-4444-555555555555";
        underTest.addParty(PartyEntity.builder()
            .id(UUID.fromString(partyId))
            .build());

        // when
        underTest.addParty(PartyEntity.builder()
            .id(UUID.fromString(partyId))
            .build());

        // then
        assertThat(underTest.getClaimPartyOrganisationList()).hasSize(1);
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
