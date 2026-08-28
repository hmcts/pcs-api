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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
        Set<UUID> represented = underTest.resolveRepresentedPartyIds(List.of(party), USER_ID);

        // Then
        assertThat(represented).containsExactly(party.getId());
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldOwnPartyWhenUsersOrganisationRepresentsTheParty() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        Set<UUID> represented = underTest.resolveRepresentedPartyIds(List.of(party), USER_ID);

        // Then
        assertThat(represented).containsExactly(party.getId());
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

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), USER_ID)).isEmpty();
    }

    @Test
    void shouldNotOwnPartyWhenRepresentationIsNotActive() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.NO);

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), USER_ID)).isEmpty();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldNotOwnPartyOnTheOtherSide() {
        // Given
        PartyEntity party = partyRepresentedBy("OTHER-ORG", YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), USER_ID)).isEmpty();
    }

    @Test
    void shouldNotResolveAnOrganisationForACitizenUser() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(null);

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), USER_ID)).isEmpty();
    }

    @Test
    void shouldNotOwnPartyWhenTheRepresentingOrganisationHasNoIdentifier() {
        // Given
        PartyEntity party = partyRepresentedBy(null, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), USER_ID)).isEmpty();
    }

    @Test
    void shouldNotLookUpAnOrganisationWhenPartyHasNoActiveRepresentation() {
        // Given
        PartyEntity party = partyWithoutRepresentation(null);

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), USER_ID)).isEmpty();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldOwnPartyCreatedByTheUsersOwnOrganisation() {
        // Given
        PartyEntity party = partyWithoutRepresentation(ORG_ID);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), USER_ID))
            .containsExactly(party.getId());
    }

    @Test
    void shouldNotOwnPartyCreatedByADifferentOrganisation() {
        // Given
        PartyEntity party = partyWithoutRepresentation("OTHER-ORG");
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), USER_ID)).isEmpty();
    }

    @Test
    void shouldNotOwnPartyByOrganisationWhenUserHasNoOrganisation() {
        // Given
        PartyEntity party = partyWithoutRepresentation(ORG_ID);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(null);

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), USER_ID)).isEmpty();
    }

    /**
     * A request whose user identity cannot be resolved acts for nobody, so the organisation is never
     * looked up and no party is treated as owned.
     */
    @Test
    void shouldOwnNoPartyWhenThereIsNoAuthenticatedUser() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.YES);

        // When / Then
        assertThat(underTest.resolveRepresentedPartyIds(List.of(party), null)).isEmpty();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldResolveOwnershipForEveryPartyItIsGiven() {
        // Given
        PartyEntity ownParty = partyRepresentedBy(ORG_ID, YesOrNo.YES);
        PartyEntity otherSideParty = partyRepresentedBy("OTHER-ORG", YesOrNo.YES);
        PartyEntity selfParty = PartyEntity.builder().id(UUID.randomUUID()).idamId(USER_ID).build();
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        Set<UUID> represented =
            underTest.resolveRepresentedPartyIds(List.of(ownParty, otherSideParty, selfParty), USER_ID);

        // Then
        assertThat(represented).containsExactlyInAnyOrder(ownParty.getId(), selfParty.getId());
    }

    @Test
    void shouldLookUpTheUsersOrganisationOnceForTheWholeCollection() {
        // Given
        List<PartyEntity> parties = List.of(
            partyRepresentedBy("OTHER-ORG", YesOrNo.YES),
            partyWithoutRepresentation("ANOTHER-ORG"),
            partyRepresentedBy(ORG_ID, YesOrNo.YES),
            partyWithoutRepresentation(ORG_ID));
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        underTest.resolveRepresentedPartyIds(parties, USER_ID);

        // Then
        verify(organisationService, times(1)).getOrganisationIdForCurrentUser();
    }

    @Test
    void shouldNotLookUpTheUsersOrganisationWhenNoPartyNeedsIt() {
        // Given
        List<PartyEntity> parties = List.of(
            PartyEntity.builder().id(UUID.randomUUID()).idamId(USER_ID).build(),
            partyWithoutRepresentation(null));

        // When
        underTest.resolveRepresentedPartyIds(parties, USER_ID);

        // Then
        verifyNoInteractions(organisationService);
    }

    private PartyEntity partyWithoutRepresentation(String organisationId) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .organisationId(organisationId)
            .claimPartyOrganisationList(new ArrayList<>())
            .build();
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
