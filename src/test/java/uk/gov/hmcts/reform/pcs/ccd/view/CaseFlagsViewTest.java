package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
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

        pcsCaseEntity.setCaseFlags(List.of(createMockCaseFlagsEntity()));

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
        PartyEntity partyEntityFirst = createPartyEntity(null);
        PartyEntity partyEntitySecond = createPartyEntity("King Smith");


        CasePartyFlagEntity defendantFlags = createMockCasePartyFlagsEntity();
        partyEntityFirst.setDefendantFlags(List.of(defendantFlags));

        PCSCase pcsCase = PCSCase.builder().build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        pcsCaseEntity.setParties(Set.of(partyEntityFirst, partyEntitySecond));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNotNull(pcsCase.getParties());
        assertEquals(1, pcsCase.getParties().size());
        Party party = pcsCase.getParties().getFirst().getValue();
        assertNotNull(party.getDefendantFlags());
        assertEquals(1, party.getDefendantFlags().getDetails().size());
        assertEquals("PF0015", party.getDefendantFlags().getDetails().getFirst().getValue().getFlagCode());
    }

    private PartyEntity createPartyEntity(String orgName) {  //, String firstName, String lastName

        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .orgName(orgName)
            .build();
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
        assertNotNull(party.getDefendantFlags());
        assertEquals(0, party.getDefendantFlags().getDetails().size());
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

    private CaseFlagEntity createMockCaseFlagsEntity() {

        CaseFlagEntity  caseFlagEntity = new CaseFlagEntity();
        caseFlagEntity.setId(UUID.randomUUID());
        caseFlagEntity.setFlagComment("Urgent case");
        caseFlagEntity.setPaths(UUID.randomUUID() + ":"  + "Case");
        caseFlagEntity.setFlagRefData(createMockRefDataFlagsEntity("CF0007", "Urgent case"));

        return  caseFlagEntity;
    }

    private CasePartyFlagEntity createMockCasePartyFlagsEntity() {

        CasePartyFlagEntity  casePartyFlagEntity = new CasePartyFlagEntity();

        casePartyFlagEntity.setId(UUID.randomUUID());
        casePartyFlagEntity.setFlagComment("Language Interpreter");
        casePartyFlagEntity.setPaths(UUID.randomUUID() + ":" + "Case");
        casePartyFlagEntity.setFlagRefData(createMockRefDataFlagsEntity("PF0015", "Language Interpreter"));

        return  casePartyFlagEntity;
    }

    private FlagRefDataEntity createMockRefDataFlagsEntity(String flagCode, String flagName) {

        return FlagRefDataEntity.builder()
            .flagCode(flagCode)
            .flagName(flagName)
            .build();
    }
}
