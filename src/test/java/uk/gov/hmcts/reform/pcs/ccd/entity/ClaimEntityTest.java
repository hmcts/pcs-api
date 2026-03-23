package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.HousingActWalesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;

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

}
