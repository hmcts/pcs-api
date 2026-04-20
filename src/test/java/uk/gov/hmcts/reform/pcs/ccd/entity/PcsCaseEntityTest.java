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

}
