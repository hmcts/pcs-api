package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        pcsCaseEntity.setCaseFlags(List.of(createMockFlagsEntity()));

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

        FlagDetailsEntity appellantFlags = createMockFlagsEntity();
        partyEntity.setRespondentFlags(List.of(appellantFlags));

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
        assertEquals(1, party.getRespondentFlags().getDetails().size());
        assertEquals("CF0007", party.getRespondentFlags().getDetails().getFirst().getValue().getFlagCode());
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

    private FlagDetailsEntity createMockFlagsEntity() {

        return FlagDetailsEntity.builder()
            .id(UUID.randomUUID())
            .flagCode("CF0007")
            .flagComment("Urgent case")
            .paths(List.of(createMockFlagPathEntity()))
            .build();
    }

    private FlagPathEntity createMockFlagPathEntity() {
        return FlagPathEntity.builder()
            .id(UUID.randomUUID())
            .path("Case")
            .build();
    }
}
