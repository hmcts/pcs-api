package uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LegalRepresentativeOrganisationEntityTest {

    private LegalRepresentativeOrganisationEntity underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepresentativeOrganisationEntity();
    }

    @Test
    void addParty_ShouldReactivatePartyIfAlreadyPresent() {
        // given
        UUID partyId = UUID.randomUUID();
        PartyEntity party = PartyEntity.builder()
            .id(partyId)
            .build();

        underTest.addParty(party);

        // when
        underTest.addParty(party);

        // verify
        assertThat(underTest.getClaimPartyLegalRepresentativeOrganisationList().size()).isEqualTo(1);

        ClaimPartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisation =
            underTest.getClaimPartyLegalRepresentativeOrganisationList().getFirst();

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
        assertThat(underTest.getClaimPartyLegalRepresentativeOrganisationList().size()).isEqualTo(2);

        ClaimPartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisation =
            underTest.getClaimPartyLegalRepresentativeOrganisationList().getFirst();

        assertThat(partyLegalRepresentativeOrganisation.getParty().getId()).isEqualTo(partyId);


        ClaimPartyLegalRepresentativeOrganisationEntity partyLegalRepresentativeOrganisation2 =
            underTest.getClaimPartyLegalRepresentativeOrganisationList().get(1);

        assertThat(partyLegalRepresentativeOrganisation2.getParty().getId()).isEqualTo(partyId2);
        assertThat(partyLegalRepresentativeOrganisation2.getLegalRepresentativeOrganisation()).isEqualTo(underTest);
        assertThat(partyLegalRepresentativeOrganisation2.getActive()).isEqualTo(YesOrNo.YES);
    }

}
