package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.enforcement.service.EvictionDetailsService.EvictionDetails;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvictionDetailsServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PcsCaseService pcsCaseService;

    @InjectMocks
    private EvictionDetailsService underTest;

    @Test
    void shouldListTheClaimantTypeClaimantsAndDefendantsInRankOrder() {
        ClaimEntity claim = new ClaimEntity();
        claim.setClaimantType(ClaimantType.PRIVATE_LANDLORD);
        claim.setClaimParties(new ArrayList<>(List.of(
            claimParty(PartyRole.DEFENDANT, 2, person("Sam", "Smith", VerticalYesNo.YES)),
            claimParty(PartyRole.CLAIMANT, 1, organisation("Acme Lettings")),
            claimParty(PartyRole.DEFENDANT, 1, person("Jo", "Bloggs", VerticalYesNo.YES)),
            claimParty(PartyRole.LITIGATION_FRIEND, 1, person("Not", "Shown", VerticalYesNo.YES))
        )));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseWith(claim));

        EvictionDetails details = underTest.getEvictionDetails(CASE_REFERENCE);

        assertThat(details.claimantType()).isEqualTo("Private landlord");
        assertThat(details.claimantNames()).containsExactly("Acme Lettings");
        assertThat(details.defendantNames()).containsExactly("Jo Bloggs", "Sam Smith");
    }

    @Test
    void shouldShowUnnamedDefendantsOnceAsPersonsUnknown() {
        ClaimEntity claim = new ClaimEntity();
        claim.setClaimParties(new ArrayList<>(List.of(
            claimParty(PartyRole.CLAIMANT, 1, person("Pat", "Landlord", VerticalYesNo.YES)),
            claimParty(PartyRole.DEFENDANT, 1, person(null, null, VerticalYesNo.NO)),
            claimParty(PartyRole.DEFENDANT, 2, person(null, null, VerticalYesNo.NO)),
            claimParty(PartyRole.DEFENDANT, 3, person("Jo", "Bloggs", VerticalYesNo.YES))
        )));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseWith(claim));

        EvictionDetails details = underTest.getEvictionDetails(CASE_REFERENCE);

        assertThat(details.claimantNames()).containsExactly("Pat Landlord");
        assertThat(details.defendantNames()).containsExactly("Person(s) unknown", "Jo Bloggs");
    }

    @Test
    void shouldLeaveOutRemovedParties() {
        ClaimEntity claim = new ClaimEntity();
        PartyEntity removed = person("Gone", "Away", VerticalYesNo.YES);
        removed.setRemoved(true);
        claim.setClaimParties(new ArrayList<>(List.of(
            claimParty(PartyRole.DEFENDANT, 1, removed),
            claimParty(PartyRole.DEFENDANT, 2, person("Jo", "Bloggs", VerticalYesNo.YES))
        )));
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(caseWith(claim));

        assertThat(underTest.getEvictionDetails(CASE_REFERENCE).defendantNames()).containsExactly("Jo Bloggs");
    }

    @Test
    void shouldFallBackToTheCaseClaimantTypeAndCopeWithNoClaim() {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setClaimantType(ClaimantType.MORTGAGE_LENDER);
        pcsCase.setClaims(List.of());

        EvictionDetails details = underTest.getEvictionDetails(pcsCase);

        assertThat(details.claimantType()).isEqualTo("Mortgage lender");
        assertThat(details.claimantNames()).isEmpty();
        assertThat(details.defendantNames()).isEmpty();
    }

    private static PcsCaseEntity caseWith(ClaimEntity claim) {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setClaims(List.of(claim));
        return pcsCase;
    }

    private static ClaimPartyEntity claimParty(PartyRole role, int rank, PartyEntity party) {
        return ClaimPartyEntity.builder().role(role).rank(rank).party(party).build();
    }

    private static PartyEntity person(String firstName, String lastName, VerticalYesNo nameKnown) {
        PartyEntity party = new PartyEntity();
        party.setFirstName(firstName);
        party.setLastName(lastName);
        party.setNameKnown(nameKnown);
        return party;
    }

    private static PartyEntity organisation(String name) {
        PartyEntity party = new PartyEntity();
        party.setOrgName(name);
        return party;
    }
}
