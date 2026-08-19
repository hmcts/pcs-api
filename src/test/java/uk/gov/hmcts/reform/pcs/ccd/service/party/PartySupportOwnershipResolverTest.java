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
    void shouldOwnPartyWhenUserIsTheActiveLegalRepresentative() {
        // Given
        PartyEntity party = partyWithLegalRep(USER_ID, null, YesOrNo.YES);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isTrue();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldOwnPartyWhenUsersOrganisationIsTheActiveLegalRepresentative() {
        // Given
        PartyEntity party = partyWithLegalRep(UUID.randomUUID(), ORG_ID, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isTrue();
    }

    @Test
    void shouldNotOwnPartyWhenLegalRepresentativeIsInactive() {
        // Given
        PartyEntity party = partyWithLegalRep(USER_ID, ORG_ID, YesOrNo.NO);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldNotOwnPartyOnTheOtherSide() {
        // Given
        PartyEntity party = partyWithLegalRep(UUID.randomUUID(), "OTHER-ORG", YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
    }

    @Test
    void shouldNotResolveAnOrganisationForACitizenUser() {
        // Given
        PartyEntity party = partyWithLegalRep(UUID.randomUUID(), ORG_ID, YesOrNo.YES);
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(null);

        // When
        boolean owned = underTest.isOwnedByUser(party, USER_ID);

        // Then
        assertThat(owned).isFalse();
    }

    @Test
    void shouldNotLookUpAnOrganisationWhenPartyHasNoActiveRepresentatives() {
        // Given
        PartyEntity party = PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .claimPartyLegalRepresentativeList(new ArrayList<>())
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
            .claimPartyLegalRepresentativeList(new ArrayList<>())
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
            .claimPartyLegalRepresentativeList(new ArrayList<>())
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
            .claimPartyLegalRepresentativeList(new ArrayList<>())
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
        PartyEntity defendant = defendantRepresentedBy("DEFENDANT-FIRM", UUID.randomUUID());
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(ORG_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(defendant, USER_ID)).isFalse();
    }

    @Test
    void shouldRejectClaimantSolicitorForAnUnrepresentedDefendant() {
        // Given
        PartyEntity defendant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .claimPartyLegalRepresentativeList(new ArrayList<>())
            .build();

        // When / Then
        assertThat(underTest.isOwnedByUser(defendant, USER_ID)).isFalse();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldAllowDefendantSolicitorForTheDefendantTheyRepresent() {
        // Given
        PartyEntity defendant = defendantRepresentedBy("DEFENDANT-FIRM", USER_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(defendant, USER_ID)).isTrue();
        verifyNoInteractions(organisationService);
    }

    @Test
    void shouldRejectDefendantSolicitorForTheClaimant() {
        // Given
        PartyEntity claimant = claimantRepresentedBy("CLAIMANT-FIRM");
        when(organisationService.getOrganisationIdForCurrentUser()).thenReturn("DEFENDANT-FIRM");

        // When / Then
        assertThat(underTest.isOwnedByUser(claimant, USER_ID)).isFalse();
    }

    @Test
    void shouldAllowAPartyActingForThemselvesRegardlessOfRepresentation() {
        // Given
        PartyEntity defendant = defendantRepresentedBy("DEFENDANT-FIRM", UUID.randomUUID());
        defendant.setIdamId(USER_ID);

        // When / Then
        assertThat(underTest.isOwnedByUser(defendant, USER_ID)).isTrue();
        verifyNoInteractions(organisationService);
    }

    private PartyEntity claimantRepresentedBy(String organisationId) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .idamId(UUID.randomUUID())
            .organisationId(organisationId)
            .claimPartyLegalRepresentativeList(new ArrayList<>())
            .build();
    }

    private PartyEntity defendantRepresentedBy(String organisationId, UUID legalRepIdamId) {
        return partyWithLegalRep(legalRepIdamId, organisationId, YesOrNo.YES);
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
