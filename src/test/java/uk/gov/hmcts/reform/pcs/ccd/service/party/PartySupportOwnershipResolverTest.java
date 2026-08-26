package uk.gov.hmcts.reform.pcs.ccd.service.party;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartySupportOwnershipResolverTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ORG_ID = "ORG-1";

    @Mock
    private OrganisationService organisationService;

    @InjectMocks
    private PartySupportOwnershipResolver underTest;

    @Test
    void shouldOwnPartyWhenUserIsThePartyThemselves() {
        // Given
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).idamId(USER_ID).build();

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isTrue();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldOwnPartyWhenUsersOrganisationRepresentsTheParty() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isTrue();
    }

    /**
     * Representation is recorded against the organisation acting for the party, so being a legal
     * representative user is not on its own enough — the acting organisation has to match.
     */
    @Test
    void shouldNotOwnPartyOnIndividualIdentityAloneWhenTheOrganisationDoesNotMatch() {
        // Given
        PartyEntity party = partyRepresentedBy("OTHER-ORG", YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
    }

    @Test
    void shouldNotOwnPartyWhenRepresentationIsNotActive() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.NO);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldNotOwnPartyOnTheOtherSide() {
        // Given
        PartyEntity party = partyRepresentedBy("OTHER-ORG", YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
    }

    @Test
    void shouldNotResolveAnOrganisationForACitizenUser() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(null);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
    }

    @Test
    void shouldNotOwnPartyWhenTheRepresentingOrganisationHasNoIdentifier() {
        // Given
        PartyEntity party = partyRepresentedBy(null, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
    }

    @Test
    void shouldNotLookUpAnOrganisationWhenPartyHasNoActiveRepresentation() {
        // Given
        PartyEntity party = PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .claimPartyOrganisationList(new ArrayList<>())
            .build();

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldOwnPartyCreatedByTheUsersOwnOrganisation() {
        // Given
        PartyEntity party = PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .organisationId(ORG_ID)
            .claimPartyOrganisationList(new ArrayList<>())
            .build();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isTrue();
    }

    @Test
    void shouldNotOwnPartyCreatedByADifferentOrganisation() {
        // Given
        PartyEntity party = PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .organisationId("OTHER-ORG")
            .claimPartyOrganisationList(new ArrayList<>())
            .build();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
    }

    @Test
    void shouldNotOwnPartyByOrganisationWhenUserHasNoOrganisation() {
        // Given
        PartyEntity party = PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .organisationId(ORG_ID)
            .claimPartyOrganisationList(new ArrayList<>())
            .build();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(null);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
    }

    @Test
    void shouldNotOwnPartyWhenUserOrPartyIsNull() {
        // Given
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).build();

        // When / Then
        assertThat(underTest.isOwnedByUser(party, null)).isFalse();
        assertThat(underTest.isOwnedByUser(null, USER_ID)).isFalse();
        verifyNoInteractions(organisationService);
    }

    private PartyEntity partyRepresentedBy(String organisationId, YesOrNo active) {
        OrganisationEntity organisation = OrganisationEntity.builder()
            .organisationId(organisationId)
            .organisationName("Test Firm")
            .build();

        ClaimPartyOrganisationEntity claimPartyOrganisation = ClaimPartyOrganisationEntity.builder()
            .organisation(organisation)
            .active(active)
            .build();

        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .claimPartyOrganisationList(new ArrayList<>(List.of(claimPartyOrganisation)))
            .build();
    }
}
