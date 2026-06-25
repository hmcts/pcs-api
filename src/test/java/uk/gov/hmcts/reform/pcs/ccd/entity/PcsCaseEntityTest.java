package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PcsCaseEntityTest {

    private PcsCaseEntity underTest;

    @BeforeEach
    void setUp() {
        underTest = new PcsCaseEntity();
    }

    @Test
    void shouldUpdateCaseOnTenancyLicenceWhenSet() {
        // Given
        TenancyLicenceEntity existingTenancyLicence = mock(TenancyLicenceEntity.class);
        TenancyLicenceEntity updatedTenancyLicence = mock(TenancyLicenceEntity.class);
        underTest.setTenancyLicence(existingTenancyLicence);

        // When
        underTest.setTenancyLicence(updatedTenancyLicence);

        // Then
        verify(existingTenancyLicence).setPcsCase(null);
        verify(updatedTenancyLicence).setPcsCase(underTest);
    }

    @Test
    void shouldAddGenAppEntityAndSetCaseLevelRank() {
        // Given
        GenAppEntity genAppEntity1 = mock(GenAppEntity.class);
        GenAppEntity genAppEntity2 = mock(GenAppEntity.class);
        GenAppEntity genAppEntity3 = mock(GenAppEntity.class);

        // When
        underTest.addGenApp(genAppEntity1);
        underTest.addGenApp(genAppEntity2);
        underTest.addGenApp(genAppEntity3);

        // Then
        verify(genAppEntity1).setRank(1);
        verify(genAppEntity1).setPcsCase(underTest);

        verify(genAppEntity2).setRank(2);
        verify(genAppEntity2).setPcsCase(underTest);

        verify(genAppEntity3).setRank(3);
        verify(genAppEntity3).setPcsCase(underTest);
    }

    @Test
    void shouldContinueGenAppCaseLevelRankFromExistingApplications() {
        // Given
        for (int i = 0; i < 5; i++) {
            underTest.getGenApps().add(mock(GenAppEntity.class));
        }
        GenAppEntity genAppEntity = mock(GenAppEntity.class);

        // When
        underTest.addGenApp(genAppEntity);

        // Then
        verify(genAppEntity).setRank(6);
        verify(genAppEntity).setPcsCase(underTest);
    }

}
