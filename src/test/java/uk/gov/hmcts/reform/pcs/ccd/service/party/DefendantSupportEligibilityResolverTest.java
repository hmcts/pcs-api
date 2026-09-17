package uk.gov.hmcts.reform.pcs.ccd.service.party;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DefendantSupportEligibilityResolverTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private PartySupportOwnershipResolver partySupportOwnershipResolver;

    @InjectMocks
    private DefendantSupportEligibilityResolver underTest;

    @Test
    void shouldResolveNoPartiesWhenTheCaseIsNull() {
        assertThat(underTest.resolveEligibleDefendantPartyIds(null, USER_ID)).isEmpty();
    }

    @Test
    void shouldResolveNoPartiesWhenThereIsNoAuthenticatedUser() {
        assertThat(underTest.resolveEligibleDefendantPartyIds(new PcsCaseEntity(), null)).isEmpty();
    }

    @Test
    void shouldResolveNoPartiesWhenTheCaseHasNoClaims() {
        assertThat(underTest.resolveEligibleDefendantPartyIds(new PcsCaseEntity(), USER_ID)).isEmpty();
    }

    @Test
    void shouldResolveNoPartiesWhenTheCaseHasNoDefendant() {
        PartyEntity claimantParty = partyWithId();
        PcsCaseEntity caseEntity = caseWith(claimantParty, PartyRole.CLAIMANT);
        representing(claimantParty);

        assertThat(underTest.resolveEligibleDefendantPartyIds(caseEntity, USER_ID)).isEmpty();
    }

    @Test
    void shouldResolveOnlyDefendantPartiesTheUserRepresents() {
        PartyEntity claimantParty = partyWithId();
        PartyEntity defendantParty = partyWithId();
        PcsCaseEntity caseEntity = caseWith(claimantParty, PartyRole.CLAIMANT);
        addParty(caseEntity, defendantParty, PartyRole.DEFENDANT);
        representing(claimantParty, defendantParty);

        assertThat(underTest.resolveEligibleDefendantPartyIds(caseEntity, USER_ID))
            .containsExactly(defendantParty.getId());
    }

    @Test
    void shouldExcludeADefendantTheUserDoesNotRepresent() {
        PartyEntity claimantParty = partyWithId();
        PartyEntity defendantParty = partyWithId();
        PcsCaseEntity caseEntity = caseWith(claimantParty, PartyRole.CLAIMANT);
        addParty(caseEntity, defendantParty, PartyRole.DEFENDANT);
        representing(claimantParty);

        assertThat(underTest.resolveEligibleDefendantPartyIds(caseEntity, USER_ID)).isEmpty();
    }

    @Test
    void shouldResolveEveryRepresentedDefendantWhenMoreThanOneIsRepresented() {
        PartyEntity firstDefendant = partyWithId();
        PartyEntity secondDefendant = partyWithId();
        PcsCaseEntity caseEntity = caseWith(firstDefendant, PartyRole.DEFENDANT);
        addParty(caseEntity, secondDefendant, PartyRole.DEFENDANT);
        representing(firstDefendant, secondDefendant);

        assertThat(underTest.resolveEligibleDefendantPartyIds(caseEntity, USER_ID))
            .containsExactlyInAnyOrder(firstDefendant.getId(), secondDefendant.getId());
    }

    @Test
    void shouldIgnorePartyRolesThatAreNotClaimantOrDefendant() {
        PartyEntity defendantParty = partyWithId();
        PartyEntity otherParty = partyWithId();
        PcsCaseEntity caseEntity = caseWith(defendantParty, PartyRole.DEFENDANT);
        addParty(caseEntity, otherParty, PartyRole.UNDERLESSEE_OR_MORTGAGEE);
        representing(defendantParty, otherParty);

        assertThat(underTest.resolveEligibleDefendantPartyIds(caseEntity, USER_ID))
            .containsExactly(defendantParty.getId());
    }

    private void representing(PartyEntity... parties) {
        Set<UUID> representedPartyIds = Set.of(java.util.Arrays.stream(parties)
                                                   .map(PartyEntity::getId)
                                                   .toArray(UUID[]::new));
        lenient().when(partySupportOwnershipResolver.resolveRepresentedPartyIds(any(), any()))
            .thenReturn(representedPartyIds);
    }

    private PartyEntity partyWithId() {
        PartyEntity partyEntity = PartyEntity.builder().build();
        partyEntity.setId(UUID.randomUUID());
        return partyEntity;
    }

    private PcsCaseEntity caseWith(PartyEntity partyEntity, PartyRole partyRole) {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.addClaim(ClaimEntity.builder().build());
        addParty(caseEntity, partyEntity, partyRole);
        return caseEntity;
    }

    private void addParty(PcsCaseEntity caseEntity, PartyEntity partyEntity, PartyRole partyRole) {
        caseEntity.addParty(partyEntity);
        caseEntity.getClaims().getFirst().addParty(partyEntity, partyRole);
    }
}
