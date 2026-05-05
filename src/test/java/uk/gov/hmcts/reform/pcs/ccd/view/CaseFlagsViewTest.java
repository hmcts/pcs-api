package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.*;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class CaseFlagsViewTest {

    private CaseFlagsView underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseFlagsView();
    }

    @Test
    void shouldMapBasicCaseFlagFieldsWhenCaseFlagsAreNull() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNotNull(pcsCase.getCaseFlags());
        assertNotNull(pcsCase.getCaseFlags());
        assertNull(pcsCase.getCaseFlags().getDetails());

    }

    @Test
    void shouldMapBasicCaseFlagFieldsWhenCaseFlagsExist() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        pcsCaseEntity.setCaseFlags(List.of((CaseFlagEntity)createMockCaseFlagsEntity()));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNotNull(pcsCase.getCaseFlags());
        assertEquals(1, pcsCase.getCaseFlags().getDetails().size());
        assertEquals("CF0007", pcsCase.getCaseFlags().getDetails().getFirst().getValue().getFlagCode());
    }

    @Test
    void shouldMapComplexPartyFlagFieldsWhenPartiesExist() {
        // Given
        PartyEntity partyEntity = new PartyEntity();
        partyEntity.setId(UUID.randomUUID());

        CasePartyFlagEntity appellantFlags = (CasePartyFlagEntity) createMockCaseFlagsEntity();
        partyEntity.setRespondentFlags(List.of(appellantFlags));

        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        pcsCaseEntity.setParties(Set.of(partyEntity));
        //when(refDataFlagsRepository.findAll()).thenReturn(List.of(createMockRefDataFlagsEntity()));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNotNull(pcsCase.getParties());
        assertEquals(1, pcsCase.getParties().size());
        Party party = pcsCase.getParties().getFirst().getValue();
        assertNotNull(party.getRespondentFlags());
        assertEquals(1, party.getRespondentFlags().getDetails().size());
        assertEquals("CF0007", party.getRespondentFlags().getDetails().getFirst().getValue().getFlagCode());
    }

    @Test
    void shouldMapComplexPartyFlagFieldsWhenPartiesExistsWithNoFlags() {
        // Given
        PartyEntity partyEntity = new PartyEntity();
        partyEntity.setId(UUID.randomUUID());

        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(partyEntity));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNotNull(pcsCase.getParties());
        assertEquals(1, pcsCase.getParties().size());
        Party party = pcsCase.getParties().getFirst().getValue();
        assertNotNull(party.getRespondentFlags());
        assertEquals(0, party.getRespondentFlags().getDetails().size());
    }

    @Test
    void shouldHandleNullCaseFlagsGracefully() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNull(pcsCase.getCaseFlags().getDetails());
    }

    @Test
    void shouldHandleNullPartiesGracefully() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertEquals(0, pcsCase.getParties().size());
    }

    private BaseCaseFlag createMockCaseFlagsEntity() {

        CaseFlagEntity  caseFlagEntity = new CaseFlagEntity();

        caseFlagEntity.setId(UUID.randomUUID());
        caseFlagEntity.setFlagCode("CF0007");
        caseFlagEntity.setFlagComment("Urgent case");
        caseFlagEntity.setPaths(List.of(createMockFlagPathEntity()));
        caseFlagEntity.setRefDataFlag(createMockRefDataFlagsEntity());

        return  caseFlagEntity;
    }

    private FlagPathEntity createMockFlagPathEntity() {
        return FlagPathEntity.builder()
            .id(UUID.randomUUID())
            .path("Case")
            .build();
    }

    private RefDataFlagEntity createMockRefDataFlagsEntity() {

        return RefDataFlagEntity.builder()
            .flagCode("CF0007")
            .flagName("Urgent case")
            .build();
    }
}
