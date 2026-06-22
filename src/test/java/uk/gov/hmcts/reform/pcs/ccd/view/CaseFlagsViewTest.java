package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyId;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

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
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertNotNull(pcsCase.getCaseFlags());
        assertNotNull(pcsCase.getCaseFlags());
        assertNull(pcsCase.getCaseFlags().getDetails());
    }

    @Test
    void shouldMapBasicCaseFlagFieldsWhenCaseFlagsExist() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        pcsCaseEntity.setCaseFlags(List.of(createMockCaseFlagsEntity()));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertNotNull(pcsCase.getCaseFlags());
        assertEquals(1, pcsCase.getCaseFlags().getDetails().size());
        assertEquals("CF0007", pcsCase.getCaseFlags().getDetails().getFirst().getValue().getFlagCode());
    }

    @Test
    void shouldMapComplexPartyFlagFieldsWhenPartiesExist() {
        PartyEntity defendantEntity = createPartyEntity(null);
        defendantEntity.setDefendantFlags(List.of(createMockCasePartyFlagsEntity()));

        PartyEntity orgEntity = createPartyEntity("King Smith");

        Set<PartyEntity> partyEntities = Set.of(defendantEntity, orgEntity);

        PCSCase pcsCase = PCSCase.builder()
            .parties(partyEntities.stream().map(this::mappedParty).toList())
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(partyEntities);
        setClaimParties(pcsCaseEntity, createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

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
        PartyEntity individualUnderlessee = PartyEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Under")
            .lastName("Lessee")
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(individualUnderlessee)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(individualUnderlessee));
        setClaimParties(pcsCaseEntity, createClaimParty(individualUnderlessee, PartyRole.UNDERLESSEE_OR_MORTGAGEE));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertEquals(1, pcsCase.getParties().size());
        Party mapped = pcsCase.getParties().getFirst().getValue();
        assertNull(mapped.getDefendantFlags());
        assertEquals("Under", mapped.getFirstName());
        assertEquals("Lessee", mapped.getLastName());
    }

    @Test
    void shouldNotMapDefendantFlagsWhenNoClaimsExist() {
        PartyEntity partyEntity = createPartyEntity(null);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(partyEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(partyEntity));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mapped = pcsCase.getParties().getFirst().getValue();
        assertNull(mapped.getDefendantFlags());
    }

    @Test
    void shouldUsePartyEntityIdWhenClaimPartyEmbeddedIdHasNoPartyId() {
        PartyEntity defendantEntity = createPartyEntity(null);
        ClaimPartyEntity claimParty = ClaimPartyEntity.builder()
            .id(new ClaimPartyId())
            .party(defendantEntity)
            .role(PartyRole.DEFENDANT)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(defendantEntity));
        setClaimParties(pcsCaseEntity, claimParty);

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mapped = pcsCase.getParties().getFirst().getValue();
        assertNotNull(mapped.getDefendantFlags());
        assertEquals(0, mapped.getDefendantFlags().getDetails().size());
    }

    @Test
    void shouldIgnoreDefendantClaimPartyWhenNoPartyIdIsAvailable() {
        PartyEntity partyEntity = createPartyEntity(null);
        ClaimPartyEntity claimParty = ClaimPartyEntity.builder()
            .id(new ClaimPartyId())
            .role(PartyRole.DEFENDANT)
            .build();

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(partyEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(partyEntity));
        setClaimParties(pcsCaseEntity, claimParty);

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mapped = pcsCase.getParties().getFirst().getValue();
        assertNull(mapped.getDefendantFlags());
    }

    @Test
    void shouldMapComplexPartyFlagFieldsWhenPartiesExistsWithNoFlags() {
        PartyEntity defendantEntity = createPartyEntity(null);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(defendantEntity));
        setClaimParties(pcsCaseEntity, createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertNotNull(pcsCase.getParties());
        assertEquals(1, pcsCase.getParties().size());
        Party party = pcsCase.getParties().getFirst().getValue();
        assertNotNull(party.getDefendantFlags());
        assertEquals(0, party.getDefendantFlags().getDetails().size());
    }

    @Test
    void shouldHandleNullCaseFlagsGracefully() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertNull(pcsCase.getCaseFlags().getDetails());
    }

    @Test
    void shouldHandleNullPartiesGracefully() {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        PCSCase pcsCase = PCSCase.builder().build();

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertNull(pcsCase.getParties());
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
        return ListValue.<Party>builder()
            .value(Party.builder()
                .orgName(entity.getOrgName())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .build())
            .build();
    }

    private void setClaimParties(PcsCaseEntity pcsCaseEntity, ClaimPartyEntity... claimParties) {
        UUID claimId = UUID.randomUUID();
        ClaimEntity claim = ClaimEntity.builder()
            .id(claimId)
            .claimParties(List.of(claimParties))
            .build();

        for (ClaimPartyEntity claimParty : claimParties) {
            claimParty.getId().setClaimId(claimId);
        }

        pcsCaseEntity.setClaims(List.of(claim));
    }

    private ClaimPartyEntity createClaimParty(PartyEntity partyEntity, PartyRole role) {
        ClaimPartyId id = new ClaimPartyId();
        id.setPartyId(partyEntity.getId());

        return ClaimPartyEntity.builder()
            .id(id)
            .party(partyEntity)
            .role(role)
            .build();
    }

    private CaseFlagEntity createMockCaseFlagsEntity() {
        CaseFlagEntity caseFlagEntity = new CaseFlagEntity();
        caseFlagEntity.setId(UUID.randomUUID());
        caseFlagEntity.setFlagComment("Urgent case");
        caseFlagEntity.setPaths(UUID.randomUUID() + ":" + "Case");
        caseFlagEntity.setFlagRefData(createMockRefDataFlagsEntity("CF0007", "Urgent case"));

        return caseFlagEntity;
    }

    private CasePartyFlagEntity createMockCasePartyFlagsEntity() {
        CasePartyFlagEntity casePartyFlagEntity = new CasePartyFlagEntity();

        casePartyFlagEntity.setId(UUID.randomUUID());
        casePartyFlagEntity.setFlagComment("Language Interpreter");
        casePartyFlagEntity.setPaths(UUID.randomUUID() + ":" + "Case");
        casePartyFlagEntity.setFlagRefData(createMockRefDataFlagsEntity("PF0015", "Language Interpreter"));

        return casePartyFlagEntity;
    }

    private FlagRefDataEntity createMockRefDataFlagsEntity(String flagCode, String flagName) {
        return FlagRefDataEntity.builder()
            .flagCode(flagCode)
            .flagName(flagName)
            .build();
    }
}
