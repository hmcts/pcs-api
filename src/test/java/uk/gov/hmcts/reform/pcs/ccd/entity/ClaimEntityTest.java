package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.HousingActWalesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClaimEntityTest {

    private ClaimEntity underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClaimEntity();
    }

    @Test
    void shouldUpdateExistingHousingActWales() {
        // Given
        HousingActWalesEntity existingHousingActWales = mock(HousingActWalesEntity.class);
        HousingActWalesEntity updatedHousingActWales = mock(HousingActWalesEntity.class);
        underTest.setHousingActWales(existingHousingActWales);

        // When
        underTest.setHousingActWales(updatedHousingActWales);

        // Then
        verify(existingHousingActWales).setClaim(null);
        verify(updatedHousingActWales).setClaim(underTest);
    }

    @Test
    void shouldUpdateExistingAsbProhibitedConductEntity() {
        // Given
        AsbProhibitedConductEntity existingAsbProhibitedConduct = mock(AsbProhibitedConductEntity.class);
        AsbProhibitedConductEntity updatedAsbProhibitedConduct = mock(AsbProhibitedConductEntity.class);
        underTest.setAsbProhibitedConductEntity(existingAsbProhibitedConduct);

        // When
        underTest.setAsbProhibitedConductEntity(updatedAsbProhibitedConduct);

        // Then
        verify(existingAsbProhibitedConduct).setClaim(null);
        verify(updatedAsbProhibitedConduct).setClaim(underTest);
    }

    @Test
    void shouldUpdateClaimOnPossessionAlternatives() {
        // Given
        PossessionAlternativesEntity existingPossessionAlternatives = mock(PossessionAlternativesEntity.class);
        PossessionAlternativesEntity updatedPossessionAlternatives = mock(PossessionAlternativesEntity.class);
        underTest.setPossessionAlternativesEntity(existingPossessionAlternatives);

        // When
        underTest.setPossessionAlternativesEntity(updatedPossessionAlternatives);

        // Then
        verify(existingPossessionAlternatives).setClaim(null);
        verify(updatedPossessionAlternatives).setClaim(underTest);
    }

    @Test
    void shouldUpdateClaimOnRentArrears() {
        // Given
        RentArrearsEntity existingRentArrears = mock(RentArrearsEntity.class);
        RentArrearsEntity updatedRentArrears = mock(RentArrearsEntity.class);
        underTest.setRentArrears(existingRentArrears);

        // When
        underTest.setRentArrears(updatedRentArrears);

        // Then
        verify(existingRentArrears).setClaim(null);
        verify(updatedRentArrears).setClaim(underTest);
    }

    @Test
    void shouldUpdateClaimOnNoticeOfPossession() {
        // Given
        NoticeOfPossessionEntity existingNoticeOfPossession = mock(NoticeOfPossessionEntity.class);
        NoticeOfPossessionEntity updatedNoticeOfPossession = mock(NoticeOfPossessionEntity.class);
        underTest.setNoticeOfPossession(existingNoticeOfPossession);

        // When
        underTest.setNoticeOfPossession(updatedNoticeOfPossession);

        // Then
        verify(existingNoticeOfPossession).setClaim(null);
        verify(updatedNoticeOfPossession).setClaim(underTest);
    }

    @Test
    void shouldUpdateClaimOnStatementOfTruth() {
        // Given
        StatementOfTruthEntity existingStatementOfTruth = mock(StatementOfTruthEntity.class);
        StatementOfTruthEntity updatedStatementOfTruth = mock(StatementOfTruthEntity.class);
        underTest.setStatementOfTruth(existingStatementOfTruth);

        // When
        underTest.setStatementOfTruth(updatedStatementOfTruth);

        // Then
        verify(existingStatementOfTruth).setClaim(null);
        verify(updatedStatementOfTruth).setClaim(underTest);
    }

    @Test
    void shouldAddPartiesWithRoleAndRank() {
        // Given
        PartyEntity party1 = mock(PartyEntity.class);
        PartyEntity party2 = mock(PartyEntity.class);
        PartyEntity party3 = mock(PartyEntity.class);

        // When
        underTest.addParty(party1, PartyRole.CLAIMANT);
        underTest.addParty(party2, PartyRole.DEFENDANT);
        underTest.addParty(party3, PartyRole.DEFENDANT);

        // Then
        List<ClaimPartyEntity> claimParties = underTest.getClaimParties();

        assertThat(claimParties.get(0).getRole()).isEqualTo(PartyRole.CLAIMANT);
        assertThat(claimParties.get(0).getRank()).isEqualTo(1);

        assertThat(claimParties.get(1).getRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(claimParties.get(1).getRank()).isEqualTo(1);

        assertThat(claimParties.get(2).getRole()).isEqualTo(PartyRole.DEFENDANT);
        assertThat(claimParties.get(2).getRank()).isEqualTo(2);
    }

}
