package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
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
        // Given - a defendant (with flags) and a non-defendant organisation party.
        // The case parties are wrapped from the same entity set (as PCSCaseView does),
        // with the entity id dropped during mapping, so the two collections share order.
        PartyEntity defendantEntity = createPartyEntity(null);
        defendantEntity.setDefendantFlags(List.of(createMockCasePartyFlagsEntity()));

        PartyEntity orgEntity = createPartyEntity("King Smith");

        Set<PartyEntity> partyEntities = Set.of(defendantEntity, orgEntity);

        PCSCase pcsCase = PCSCase.builder()
            .parties(partyEntities.stream().map(this::mappedParty).toList())
            .allDefendants(List.of(mappedPartyWithId(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(partyEntities);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then - both parties remain, each ListValue now carries its entity id,
        // but only the defendant is given flags
        assertNotNull(pcsCase.getParties());
        assertEquals(2, pcsCase.getParties().size());

        Party mappedDefendant = findPartyById(pcsCase, defendantEntity.getId().toString());
        assertNotNull(mappedDefendant.getDefendantFlags());
        assertEquals(1, mappedDefendant.getDefendantFlags().getDetails().size());
        assertEquals("PF0015",
            mappedDefendant.getDefendantFlags().getDetails().getFirst().getValue().getFlagCode());

        Party mappedOrgParty = findPartyById(pcsCase, orgEntity.getId().toString());
        assertNull(mappedOrgParty.getDefendantFlags());
    }

    @Test
    void shouldNotMapDefendantFlagsForNonDefendantIndividual() {
        // Given - an individual (no orgName) who is an underlessee, not a defendant
        PartyEntity individualUnderlessee = PartyEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Under")
            .lastName("Lessee")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(individualUnderlessee)))
            .allUnderlesseeOrMortgagees(List.of(mappedPartyWithId(individualUnderlessee)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(individualUnderlessee));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then - retained, but no defendant flags applied
        assertEquals(1, pcsCase.getParties().size());
        Party mapped = pcsCase.getParties().getFirst().getValue();
        assertNull(mapped.getDefendantFlags());
        assertEquals("Under", mapped.getFirstName());
        assertEquals("Lessee", mapped.getLastName());
    }

    private Party findPartyById(PCSCase pcsCase, String id) {
        return pcsCase.getParties().stream()
            .filter(partyListValue -> id.equals(partyListValue.getId()))
            .map(ListValue::getValue)
            .findFirst()
            .orElseThrow();
    }

    private PartyEntity createPartyEntity(String orgName) {

        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .orgName(orgName)
            .build();
    }

    private ListValue<Party> mappedParty(PartyEntity entity) {
        // Mirrors PCSCaseView.mapAndWrapParties: the entity id is NOT carried onto the
        // domain Party or the ListValue - CaseFlagsView is responsible for attaching it.
        return ListValue.<Party>builder()
            .value(Party.builder()
                .orgName(entity.getOrgName())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .build())
            .build();
    }

    private ListValue<Party> mappedPartyWithId(PartyEntity entity) {
        ListValue<Party> listValue = mappedParty(entity);
        listValue.setId(entity.getId().toString());
        return listValue;
    }

    @Test
    void shouldMapComplexPartyFlagFieldsWhenPartiesExistsWithNoFlags() {
        // Given - a defendant with no party flags
        PartyEntity defendantEntity = createPartyEntity(null);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(defendantEntity)))
            .allDefendants(List.of(mappedPartyWithId(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(defendantEntity));

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
        // Given - no parties have been mapped onto the case
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then - nothing to enrich, no failure
        assertNull(pcsCase.getParties());
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
