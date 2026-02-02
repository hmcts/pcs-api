package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.HousingActWalesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClaimEntityTest {

    private ClaimEntity underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClaimEntity();
    }

    @Test
    void shouldUpdateClaimOnExistingHousingActWalesWhenSet() {
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
    void shouldUpdateClaimOnPossessionAlternativesWhenSet() {
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

}
