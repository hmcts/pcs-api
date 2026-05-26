package uk.gov.hmcts.reform.pcs.ccd.service.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentNameServiceTest {

    private DocumentNameService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DocumentNameService();
    }

    @Test
    void appendGenAppPostfixIncludesRankAndPartyLabel() {
        UUID partyId = UUID.randomUUID();
        List<ClaimPartyEntity> parties = List.of(claimPartyOf(partyId, PartyRole.DEFENDANT, 1));
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        when(mainClaim.getClaimParties()).thenReturn(parties);

        GenAppEntity genApp = mock(GenAppEntity.class);
        when(genApp.getRank()).thenReturn(2);

        String renamed = underTest.appendGenAppPostfix("witness statement.pdf", genApp, mainClaim, partyId);

        assertThat(renamed).isEqualTo("witness statement GA2 - Defendant 1.pdf");
    }

    @Test
    void appendGenAppPostfixSupportsClaimantRole() {
        UUID partyId = UUID.randomUUID();
        List<ClaimPartyEntity> parties = List.of(claimPartyOf(partyId, PartyRole.CLAIMANT, 3));
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        when(mainClaim.getClaimParties()).thenReturn(parties);

        GenAppEntity genApp = mock(GenAppEntity.class);
        when(genApp.getRank()).thenReturn(1);

        String renamed = underTest.appendGenAppPostfix("evidence.docx", genApp, mainClaim, partyId);

        assertThat(renamed).isEqualTo("evidence GA1 - Claimant 3.docx");
    }

    @Test
    void appendGenAppPostfixOmitsExtensionWhenAbsent() {
        UUID partyId = UUID.randomUUID();
        List<ClaimPartyEntity> parties = List.of(claimPartyOf(partyId, PartyRole.DEFENDANT, 1));
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        when(mainClaim.getClaimParties()).thenReturn(parties);
        GenAppEntity genApp = mock(GenAppEntity.class);
        when(genApp.getRank()).thenReturn(1);

        String renamed = underTest.appendGenAppPostfix("README", genApp, mainClaim, partyId);

        assertThat(renamed).isEqualTo("README GA1 - Defendant 1");
    }

    @Test
    void appendGenAppPostfixReturnsNullWhenFilenameIsNull() {
        assertThat(underTest.appendGenAppPostfix(
            null, mock(GenAppEntity.class), mock(ClaimEntity.class), UUID.randomUUID()))
            .isNull();
    }

    @Test
    void appendPartyPostfixAppendsDefendantLabelWithoutGaSegment() {
        UUID partyId = UUID.randomUUID();
        List<ClaimPartyEntity> parties = List.of(claimPartyOf(partyId, PartyRole.DEFENDANT, 1));
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        when(mainClaim.getClaimParties()).thenReturn(parties);

        String renamed = underTest.appendPartyPostfix("statement.pdf", mainClaim, partyId);

        assertThat(renamed).isEqualTo("statement - Defendant 1.pdf");
    }

    @Test
    void appendPartyPostfixThrowsWhenPartyNotPartOfTheClaim() {
        List<ClaimPartyEntity> parties = List.of(claimPartyOf(UUID.randomUUID(), PartyRole.DEFENDANT, 1));
        ClaimEntity mainClaim = mock(ClaimEntity.class);
        when(mainClaim.getClaimParties()).thenReturn(parties);

        assertThatThrownBy(() -> underTest.appendPartyPostfix("statement.pdf", mainClaim, UUID.randomUUID()))
            .isInstanceOf(PartyNotFoundException.class);
    }

    private ClaimPartyEntity claimPartyOf(UUID partyId, PartyRole role, int rank) {
        ClaimPartyEntity claimParty = mock(ClaimPartyEntity.class);
        PartyEntity party = mock(PartyEntity.class);
        when(party.getId()).thenReturn(partyId);
        when(claimParty.getParty()).thenReturn(party);
        when(claimParty.getRole()).thenReturn(role);
        when(claimParty.getRank()).thenReturn(rank);
        return claimParty;
    }
}
