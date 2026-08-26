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
import java.util.LinkedHashSet;
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
    private static final String CLAIMANT_FIRM = "CLAIMANT-FIRM";
    private static final String DEFENDANT_FIRM = "DEFENDANT-FIRM";

    @Mock
    private OrganisationService organisationService;

    @InjectMocks
    private PartySupportOwnershipResolver underTest;

    @Test
    void shouldOwnPartyWhenUserIsThePartyThemselves() {
        // Given
        PartyEntity party = PartyEntity.builder().id(UUID.randomUUID()).idamId(USER_ID).build();

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isTrue();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldOwnPartyWhenUsersOrganisationRepresentsTheParty() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isTrue();
    }

    /**
     * Representation is recorded against the organisation acting for the party rather than against an
     * individual representative, so being a legal representative user is not on its own enough — the
     * acting organisation has to match.
     */
    @Test
    void shouldNotOwnPartyOnIndividualIdentityAloneWhenTheOrganisationDoesNotMatch() {
        // Given
        PartyEntity party = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldNotOwnPartyWhenRepresentationIsNotActive() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.NO);

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldNotOwnPartyOnTheOtherSide() {
        // Given
        PartyEntity party = partyRepresentedBy("OTHER-ORG", YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldNotResolveAnOrganisationForACitizenUser() {
        // Given
        PartyEntity party = partyRepresentedBy(ORG_ID, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(null);

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldNotOwnPartyWhenTheRepresentingOrganisationHasNoIdentifier() {
        // Given
        PartyEntity party = partyRepresentedBy(null, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldNotOwnPartyWhenPartyHasNoActiveRepresentation() {
        // Given
        PartyEntity party = unrepresentedParty();

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldOwnPartyCreatedByTheUsersOwnOrganisation() {
        // Given
        PartyEntity party = claimantRepresentedBy(ORG_ID);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isTrue();
    }

    @Test
    void shouldNotOwnPartyCreatedByADifferentOrganisation() {
        // Given
        PartyEntity party = claimantRepresentedBy("OTHER-ORG");
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
    }

    @Test
    void shouldNotOwnPartyByOrganisationWhenUserHasNoOrganisation() {
        // Given
        PartyEntity party = claimantRepresentedBy(ORG_ID);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(null);

        // When / Then
        assertThat(underTest.isOwnedByUser(party, USER_ID)).isFalse();
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

    @Test
    void shouldAllowClaimantSolicitorForTheClaimantTheirFirmIssuedTheClaimFor() {
        // Given
        PartyEntity claimant = claimantRepresentedBy(ORG_ID);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(claimant, USER_ID)).isTrue();
    }

    @Test
    void shouldRejectClaimantSolicitorForADefendantRepresentedByAnotherFirm() {
        // Given
        PartyEntity defendant = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);

        // When / Then
        assertThat(underTest.isOwnedByUser(defendant, USER_ID)).isFalse();
    }

    @Test
    void shouldRejectClaimantSolicitorForAnUnrepresentedDefendant() {
        // Given
        PartyEntity defendant = unrepresentedParty();

        // When / Then
        assertThat(underTest.isOwnedByUser(defendant, USER_ID)).isFalse();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldAllowDefendantSolicitorForTheDefendantTheyRepresent() {
        // Given
        PartyEntity defendant = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(DEFENDANT_FIRM);

        // When / Then
        assertThat(underTest.isOwnedByUser(defendant, USER_ID)).isTrue();
    }

    @Test
    void shouldRejectDefendantSolicitorForTheClaimant() {
        // Given
        PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(DEFENDANT_FIRM);

        // When / Then
        assertThat(underTest.isOwnedByUser(claimant, USER_ID)).isFalse();
    }

    @Test
    void shouldAllowAPartyActingForThemselvesRegardlessOfRepresentation() {
        // Given
        PartyEntity defendant = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.YES);
        defendant.setIdamId(USER_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(defendant, USER_ID)).isTrue();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldResolveEveryPartyRepresentedByTheUsersOrganisation() {
        PartyEntity firstClaimant = claimantRepresentedBy(CLAIMANT_FIRM);
        PartyEntity secondClaimant = claimantRepresentedBy(CLAIMANT_FIRM);
        PartyEntity defendant = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);

        Set<UUID> representedPartyIds = underTest.resolveRepresentedPartyIds(
            new LinkedHashSet<>(List.of(firstClaimant, secondClaimant, defendant)), USER_ID);

        assertThat(representedPartyIds)
            .containsExactlyInAnyOrder(firstClaimant.getId(), secondClaimant.getId());
    }

    @Test
    void shouldResolveNoPartiesForAProfessionalFromAnUnrelatedOrganisation() {
        PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);
        PartyEntity defendant = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn("UNRELATED-FIRM");

        Set<UUID> representedPartyIds = underTest.resolveRepresentedPartyIds(
            new LinkedHashSet<>(List.of(claimant, defendant)), USER_ID);

        assertThat(representedPartyIds).isEmpty();
    }

    @Test
    void shouldResolveThePartyAUserIsActingForThemselves() {
        PartyEntity self = unrepresentedParty();
        self.setIdamId(USER_ID);
        PartyEntity otherParty = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.YES);

        Set<UUID> representedPartyIds = underTest.resolveRepresentedPartyIds(
            new LinkedHashSet<>(List.of(self, otherParty)), USER_ID);

        assertThat(representedPartyIds).containsExactly(self.getId());
    }

    @Test
    void shouldResolveNoPartiesWhenRepresentationHasEnded() {
        PartyEntity formerlyRepresented = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.NO);

        Set<UUID> representedPartyIds = underTest.resolveRepresentedPartyIds(
            new LinkedHashSet<>(List.of(formerlyRepresented)), USER_ID);

        assertThat(representedPartyIds).isEmpty();
    }

    @Test
    void shouldResolveNoPartiesWithoutAnAuthenticatedUser() {
        PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);

        assertThat(underTest.resolveRepresentedPartyIds(new LinkedHashSet<>(List.of(claimant)), null)).isEmpty();
        assertThat(underTest.resolveRepresentedPartyIds(null, USER_ID)).isEmpty();
        assertThat(underTest.resolveRepresentedPartyIds(Set.of(), USER_ID)).isEmpty();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldNotLookUpTheOrganisationWhenNoPartyHoldsOrganisationData() {
        PartyEntity self = unrepresentedParty();
        self.setIdamId(USER_ID);
        PartyEntity unrepresented = unrepresentedParty();

        Set<UUID> representedPartyIds = underTest.resolveRepresentedPartyIds(
            new LinkedHashSet<>(List.of(self, unrepresented)), USER_ID);

        assertThat(representedPartyIds).containsExactly(self.getId());
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldNotLookUpTheOrganisationForAnUnrepresentedParty() {
        assertThat(underTest.isOwnedByUser(unrepresentedParty(), USER_ID)).isFalse();

        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldLookUpTheUsersOrganisationOnceForTheWholeCase() {
        PartyEntity firstParty = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.YES);
        PartyEntity secondParty = partyRepresentedBy(DEFENDANT_FIRM, YesOrNo.YES);
        PartyEntity thirdParty = claimantRepresentedBy(CLAIMANT_FIRM);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(DEFENDANT_FIRM);

        underTest.resolveRepresentedPartyIds(
            new LinkedHashSet<>(List.of(firstParty, secondParty, thirdParty)), USER_ID);

        verify(organisationService, times(1)).getOrganisationIdForCurrentUser();
    }

    private PartyEntity claimantRepresentedBy(String organisationId) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .organisationId(organisationId)
            .claimPartyOrganisationList(new ArrayList<>())
            .build();
    }

    private PartyEntity unrepresentedParty() {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
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
