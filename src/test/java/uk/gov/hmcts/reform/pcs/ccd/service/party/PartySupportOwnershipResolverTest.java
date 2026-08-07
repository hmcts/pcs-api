package uk.gov.hmcts.reform.pcs.ccd.service.party;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyLegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PartySupportOwnershipResolverTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ORG_ID = "ORG-1";

    @Mock
    private OrganisationDetailsService organisationDetailsService;

    @InjectMocks
    private PartySupportOwnershipResolver underTest;

    @Test
    void shouldOwnPartyWhenUserIsThePartyThemselves() {
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).idamId(USER_ID).build();

        assertThat(underTest.isOwnedByUser(party, USER_ID)).isTrue();
    }

    @Test
    void shouldOwnPartyWhenUserIsTheActiveLegalRepresentative() {
        PartyEntity party = partyWithLegalRep(USER_ID, null, YesOrNo.YES);
        stubOrganisation(null);

        assertThat(underTest.isOwnedByUser(party, USER_ID)).isTrue();
    }

    @Test
    void shouldOwnPartyWhenUsersOrganisationIsTheActiveLegalRepresentative() {
        PartyEntity party = partyWithLegalRep(UUID.randomUUID(), ORG_ID, YesOrNo.YES);
        stubOrganisation(ORG_ID);

        assertThat(underTest.isOwnedByUser(party, USER_ID)).isTrue();
    }

    @Test
    void shouldNotOwnPartyWhenLegalRepresentativeIsInactive() {
        PartyEntity party = partyWithLegalRep(USER_ID, ORG_ID, YesOrNo.NO);
        stubOrganisation(ORG_ID);

        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldNotOwnPartyOnTheOtherSide() {
        PartyEntity party = partyWithLegalRep(UUID.randomUUID(), "OTHER-ORG", YesOrNo.YES);
        stubOrganisation(ORG_ID);

        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldNotOwnPartyWhenPartyHasNoRepresentativesAndDifferentIdamId() {
        PartyEntity party = PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .claimPartyLegalRepresentativeList(new ArrayList<>())
            .build();

        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldNotOwnPartyWhenUserOrPartyIsNull() {
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();

        assertThat(underTest.isOwnedByUser(party, null)).isFalse();
        assertThat(underTest.isOwnedByUser(null, USER_ID)).isFalse();
    }

    private void stubOrganisation(String organisationId) {
        lenient().when(organisationDetailsService.getOrganisationIdentifier(USER_ID.toString()))
            .thenReturn(organisationId);
    }

    private PartyEntity partyWithLegalRep(UUID legalRepIdamId, String organisationId, YesOrNo active) {
        LegalRepresentativeEntity legalRepresentative = LegalRepresentativeEntity.builder()
            .id(UUID.randomUUID())
            .idamId(legalRepIdamId)
            .organisationId(organisationId)
            .build();

        ClaimPartyLegalRepresentativeEntity claimPartyLegalRepresentative =
            ClaimPartyLegalRepresentativeEntity.builder()
                .legalRepresentative(legalRepresentative)
                .active(active)
                .build();

        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .claimPartyLegalRepresentativeList(new ArrayList<>(List.of(claimPartyLegalRepresentative)))
            .build();
    }
}
