package uk.gov.hmcts.reform.pcs.ccd.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void shouldAddGenAppEntityAndSetRank() {
        // Given
        PartyEntity party1 = mock(PartyEntity.class);
        when(party1.getId()).thenReturn(UUID.randomUUID());
        PartyEntity party2 = mock(PartyEntity.class);
        when(party2.getId()).thenReturn(UUID.randomUUID());

        GenAppEntity genAppEntity1 = mock(GenAppEntity.class);
        when(genAppEntity1.getParty()).thenReturn(party1);
        GenAppEntity genAppEntity2 = mock(GenAppEntity.class);
        Mockito.when(genAppEntity2.getParty()).thenReturn(party1);
        GenAppEntity genAppEntity3 = mock(GenAppEntity.class);
        Mockito.when(genAppEntity3.getParty()).thenReturn(party2);

        // When
        underTest.addGenApp(genAppEntity1);
        underTest.addGenApp(genAppEntity2);
        underTest.addGenApp(genAppEntity3);

        // Then
        verify(genAppEntity1).setRank(1);
        verify(genAppEntity1).setPcsCase(underTest);

        verify(genAppEntity2).setRank(2);
        verify(genAppEntity2).setPcsCase(underTest);

        verify(genAppEntity3).setRank(1);
        verify(genAppEntity3).setPcsCase(underTest);
    }

}
