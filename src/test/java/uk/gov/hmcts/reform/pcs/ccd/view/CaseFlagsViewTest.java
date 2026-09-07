package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyId;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartySupportOwnershipResolver;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseFlagsViewTest {

    private static final UUID AUTHENTICATED_USER_ID = UUID.randomUUID();

    @Mock
    private PartySupportOwnershipResolver partySupportOwnershipResolver;

    @Mock
    private SecurityContextService securityContextService;

    private CaseFlagsView underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseFlagsView(partySupportOwnershipResolver, securityContextService);
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
        assertNotNull(party.getPartyFlagsExternal());
        assertEquals(0, party.getPartyFlagsExternal().getDetails().size());
    }

    @Test
    void shouldSplitPartyFlagsIntoInternalAndExternalCollections() {
        CasePartyFlagEntity internalFlag = createMockCasePartyFlagsEntity();
        internalFlag.setVisibility("Internal");
        CasePartyFlagEntity externalFlag = createMockCasePartyFlagsEntity();
        externalFlag.setVisibility("External");
        externalFlag.setFlagRefData(createMockRefDataFlagsEntity("RA0042", "Reasonable adjustment"));

        PartyEntity defendantEntity = createPartyEntity(null);
        defendantEntity.setDefendantFlags(List.of(internalFlag, externalFlag));

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(defendantEntity));
        setClaimParties(pcsCaseEntity, createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mappedDefendant = pcsCase.getParties().getFirst().getValue();

        assertEquals(1, mappedDefendant.getDefendantFlags().getDetails().size());
        assertEquals("PF0015",
            mappedDefendant.getDefendantFlags().getDetails().getFirst().getValue().getFlagCode());
        assertEquals(FlagVisibility.INTERNAL, mappedDefendant.getDefendantFlags().getVisibility());

        assertEquals(1, mappedDefendant.getPartyFlagsExternal().getDetails().size());
        assertEquals("RA0042",
            mappedDefendant.getPartyFlagsExternal().getDetails().getFirst().getValue().getFlagCode());
        assertEquals(FlagVisibility.EXTERNAL, mappedDefendant.getPartyFlagsExternal().getVisibility());
    }

    @Test
    void shouldGroupInternalAndExternalPartyFlagsByPartyId() {
        PartyEntity defendantEntity = createPartyEntity(null);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(defendantEntity));
        setClaimParties(pcsCaseEntity, createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mappedDefendant = pcsCase.getParties().getFirst().getValue();

        assertEquals(defendantEntity.getId(), mappedDefendant.getDefendantFlags().getGroupId());
        assertEquals(defendantEntity.getId(), mappedDefendant.getPartyFlagsExternal().getGroupId());
    }

    @Test
    void shouldTreatPartyFlagsWithNoVisibilityAsInternal() {
        PartyEntity defendantEntity = createPartyEntity(null);
        defendantEntity.setDefendantFlags(List.of(createMockCasePartyFlagsEntity()));

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(defendantEntity));
        setClaimParties(pcsCaseEntity, createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mappedDefendant = pcsCase.getParties().getFirst().getValue();

        assertEquals(1, mappedDefendant.getDefendantFlags().getDetails().size());
        assertEquals(0, mappedDefendant.getPartyFlagsExternal().getDetails().size());
    }

    @Test
    void shouldKeepPathValuesContainingThePathDelimiter() {

        CaseFlagEntity caseFlagEntity = new CaseFlagEntity();
        caseFlagEntity.setId(UUID.randomUUID());
        caseFlagEntity.setFlagRefData(createMockRefDataFlagsEntity("RA0035", "Video hearing"));
        caseFlagEntity.setPaths(":Party_:Note: I need a video hearing");

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseFlags(List.of(caseFlagEntity));

        PCSCase pcsCase = PCSCase.builder().build();
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        List<ListValue<String>> paths = pcsCase.getCaseFlags().getDetails().getFirst().getValue().getPath();
        assertEquals(2, paths.size());
        assertEquals("Party", paths.get(0).getValue());
        assertEquals("Note: I need a video hearing", paths.get(1).getValue());
    }

    @Test
    void shouldReturnBothOtherDescriptionsForAStoredFlag() {

        CaseFlagEntity caseFlagEntity = new CaseFlagEntity();
        caseFlagEntity.setId(UUID.randomUUID());
        caseFlagEntity.setFlagRefData(createMockRefDataFlagsEntity("OT0001", "Other"));
        caseFlagEntity.setPaths(":Case");
        caseFlagEntity.setOtherDescription("Retired judge on case");
        caseFlagEntity.setOtherDescriptionWelsh("Barnwr wedi ymddeol ar yr achos");

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseFlags(List.of(caseFlagEntity));

        PCSCase pcsCase = PCSCase.builder().build();
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        FlagDetail flagDetail = pcsCase.getCaseFlags().getDetails().getFirst().getValue();
        assertEquals("Retired judge on case", flagDetail.getOtherDescription());
        assertEquals("Barnwr wedi ymddeol ar yr achos", flagDetail.getOtherDescriptionCy());
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

    @Test
    void shouldMapPartySupportForClaimantAndDefendantParties() {
        PartyEntity claimantEntity = createPartyEntity(null);
        PartyEntity defendantEntity = createPartyEntity(null);
        authenticatedUserRepresents(claimantEntity, defendantEntity);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(claimantEntity), mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(claimantEntity, defendantEntity)));
        setClaimParties(pcsCaseEntity,
                        createClaimParty(claimantEntity, PartyRole.CLAIMANT),
                        createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertEquals(2, pcsCase.getPartySupport().size());

        ListValue<PartySupport> claimantSupport = pcsCase.getPartySupport().getFirst();
        assertEquals(claimantEntity.getId().toString(), claimantSupport.getId());
        assertEquals("Claimant", claimantSupport.getValue().getSupportFlags().getRoleOnCase());
        assertEquals(FlagVisibility.EXTERNAL, claimantSupport.getValue().getSupportFlags().getVisibility());
        assertEquals(claimantEntity.getId(), claimantSupport.getValue().getSupportFlags().getGroupId());

        ListValue<PartySupport> defendantSupport = pcsCase.getPartySupport().get(1);
        assertEquals(defendantEntity.getId().toString(), defendantSupport.getId());
        assertEquals("Defendant", defendantSupport.getValue().getSupportFlags().getRoleOnCase());
    }

    @Test
    void shouldMapClaimantPartyFlagCollectionsWithClaimantRole() {
        CasePartyFlagEntity externalFlag = createMockCasePartyFlagsEntity();
        externalFlag.setVisibility("External");
        externalFlag.setFlagRefData(createMockRefDataFlagsEntity("RA0042", "Reasonable adjustment"));

        PartyEntity claimantEntity = createPartyEntity(null);
        claimantEntity.setDefendantFlags(List.of(externalFlag));
        authenticatedUserRepresents(claimantEntity);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(claimantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(claimantEntity)));
        setClaimParties(pcsCaseEntity, createClaimParty(claimantEntity, PartyRole.CLAIMANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mappedClaimant = pcsCase.getParties().getFirst().getValue();
        assertEquals("Claimant", mappedClaimant.getPartyFlagsExternal().getRoleOnCase());
        assertEquals(1, mappedClaimant.getPartyFlagsExternal().getDetails().size());
        assertEquals("RA0042",
            mappedClaimant.getPartyFlagsExternal().getDetails().getFirst().getValue().getFlagCode());
        assertEquals(1, pcsCase.getPartySupport().size());
        assertEquals(1, pcsCase.getPartySupport().getFirst().getValue().getSupportFlags().getDetails().size());
    }

    @Test
    void shouldExposeOnlyClaimantSupportToAUserRepresentingTheClaimant() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"));
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"));
        authenticatedUserRepresents(claimantEntity);
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        assertEquals(Set.of(claimantEntity.getId().toString()), supportPartyIds(pcsCase));
        assertEquals(Set.of("RA0042"), supportFlagCodes(pcsCase));
        assertFalse(supportFlagCodes(pcsCase).contains("RA0033"));
    }

    @Test
    void shouldExposeOnlyDefendantSupportToAUserRepresentingTheDefendant() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"));
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"));
        authenticatedUserRepresents(defendantEntity);
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        assertEquals(Set.of(defendantEntity.getId().toString()), supportPartyIds(pcsCase));
        assertEquals(Set.of("RA0033"), supportFlagCodes(pcsCase));
        assertFalse(supportFlagCodes(pcsCase).contains("RA0042"));
    }

    @Test
    void shouldExposeSupportForEveryRepresentedPartyWhenMoreThanOneIsRepresented() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"));
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"));
        authenticatedUserRepresents(claimantEntity, defendantEntity);
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        assertEquals(Set.of(claimantEntity.getId().toString(), defendantEntity.getId().toString()),
                     supportPartyIds(pcsCase));
        assertEquals(Set.of("RA0042", "RA0033"), supportFlagCodes(pcsCase));
    }

    @Test
    void shouldExposeNoSupportToAUserRepresentingNeitherParty() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"));
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"));
        authenticatedUserRepresents();
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        assertTrue(pcsCase.getPartySupport().isEmpty());
        assertTrue(supportFlagCodes(pcsCase).isEmpty());
    }

    @Test
    void shouldExposeNoSupportWhenTheReadHasNoResolvableUser() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"));
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"));
        when(securityContextService.getCurrentUserId()).thenReturn(null);
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        assertTrue(pcsCase.getPartySupport().isEmpty());
        verifyNoInteractions(partySupportOwnershipResolver);
    }

    @Test
    void shouldExposeNoSupportWhenTheSecurityContextHasNoUser() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"));
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"));
        when(securityContextService.getCurrentUserId())
            .thenThrow(new SecurityContextException("No authentication instance found"));
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        assertTrue(pcsCase.getPartySupport().isEmpty());
        verifyNoInteractions(partySupportOwnershipResolver);
    }

    @Test
    void shouldExcludeInternalFlagsFromRepresentedPartySupport() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"),
                                                     internalFlag("PF0007", "Vulnerable party"));
        PartyEntity defendantEntity = partyWithSupport(internalFlag("PF0011", "Potentially violent person"));
        authenticatedUserRepresents(claimantEntity);
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        assertEquals(Set.of("RA0042"), supportFlagCodes(pcsCase));
        assertFalse(supportFlagCodes(pcsCase).contains("PF0007"));
        assertFalse(supportFlagCodes(pcsCase).contains("PF0011"));
    }

    @Test
    void shouldExposeEveryExternalSupportTypeForARepresentedParty() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"),
                                                     externalFlag("PF0015", "Language Interpreter"),
                                                     externalFlag("SM0002", "Special measure"));
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"),
                                                      externalFlag("PF0016", "Language Interpreter"),
                                                      externalFlag("SM0003", "Special measure"));
        authenticatedUserRepresents(claimantEntity);
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        assertEquals(Set.of("RA0042", "PF0015", "SM0002"), supportFlagCodes(pcsCase));
        assertFalse(supportFlagCodes(pcsCase).contains("RA0033"));
        assertFalse(supportFlagCodes(pcsCase).contains("PF0016"));
        assertFalse(supportFlagCodes(pcsCase).contains("SM0003"));
    }

    @Test
    void shouldResolveRepresentedSupportByPartyIdRegardlessOfCollectionPosition() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"));
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"));
        authenticatedUserRepresents(claimantEntity);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(defendantEntity), mappedParty(claimantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(defendantEntity, claimantEntity)));
        setClaimParties(pcsCaseEntity,
                        createClaimParty(defendantEntity, PartyRole.DEFENDANT),
                        createClaimParty(claimantEntity, PartyRole.CLAIMANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertEquals(Set.of(claimantEntity.getId().toString()), supportPartyIds(pcsCase));
        assertEquals(Set.of("RA0042"), supportFlagCodes(pcsCase));
    }

    @Test
    void shouldExposeAnEmptySupportCollectionForARepresentedPartyWithNoSupport() {
        PartyEntity claimantEntity = partyWithSupport();
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"));
        authenticatedUserRepresents(claimantEntity);
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        assertEquals(1, pcsCase.getPartySupport().size());
        assertTrue(pcsCase.getPartySupport().getFirst().getValue().getSupportFlags().getDetails().isEmpty());
        assertTrue(supportFlagCodes(pcsCase).isEmpty());
    }

    @Test
    void shouldKeepInternalPartyFlagCollectionsCompleteForEveryParty() {
        PartyEntity claimantEntity = partyWithSupport(externalFlag("RA0042", "Reasonable adjustment"));
        PartyEntity defendantEntity = partyWithSupport(externalFlag("RA0033", "Hearing loop"));
        authenticatedUserRepresents(claimantEntity);
        PCSCase pcsCase = claimantAndDefendantCase(claimantEntity, defendantEntity);

        underTest.setCaseFields(pcsCase, pcsCaseEntityFor(claimantEntity, defendantEntity));

        Party mappedClaimant = findPartyById(pcsCase, claimantEntity.getId().toString());
        Party mappedDefendant = findPartyById(pcsCase, defendantEntity.getId().toString());

        assertEquals(1, mappedClaimant.getPartyFlagsExternal().getDetails().size());
        assertEquals(1, mappedDefendant.getPartyFlagsExternal().getDetails().size());
    }

    /**
     * Support is filtered by visibility only, never by status, so an inactivated flag stays on the Support
     * tab and simply stops counting towards the active-flag total.
     */
    @Test
    void shouldKeepInactiveSupportOnTheSupportTab() {
        CasePartyFlagEntity inactiveFlag = createMockCasePartyFlagsEntity();
        inactiveFlag.setVisibility("External");
        inactiveFlag.setDefaultStatus("Inactive");
        inactiveFlag.setFlagRefData(createMockRefDataFlagsEntity("RA0042", "Reasonable adjustment"));

        PartyEntity claimantEntity = createPartyEntity(null);
        claimantEntity.setDefendantFlags(List.of(inactiveFlag));
        authenticatedUserRepresents(claimantEntity);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(claimantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(claimantEntity)));
        setClaimParties(pcsCaseEntity, createClaimParty(claimantEntity, PartyRole.CLAIMANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Flags supportFlags = pcsCase.getPartySupport().getFirst().getValue().getSupportFlags();
        assertEquals(1, supportFlags.getDetails().size());
        assertEquals("Inactive", supportFlags.getDetails().getFirst().getValue().getStatus());
    }

    @Test
    void shouldNotMapPartySupportForOtherPartyRoles() {
        PartyEntity underlesseeEntity = createPartyEntity(null);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(underlesseeEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(underlesseeEntity)));
        setClaimParties(pcsCaseEntity,
                        createClaimParty(underlesseeEntity, PartyRole.UNDERLESSEE_OR_MORTGAGEE));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertEquals(0, pcsCase.getPartySupport().size());
        assertNull(pcsCase.getParties().getFirst().getValue().getPartyFlagsExternal());
    }

    @Test
    void shouldUseOrganisationNameForOrganisationClaimantSupport() {
        PartyEntity orgClaimant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .orgName("Anytown Housing Association")
            .build();
        authenticatedUserRepresents(orgClaimant);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(orgClaimant)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(orgClaimant)));
        setClaimParties(pcsCaseEntity, createClaimParty(orgClaimant, PartyRole.CLAIMANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertEquals("Anytown Housing Association",
            pcsCase.getPartySupport().getFirst().getValue().getSupportFlags().getPartyName());
        assertEquals("Anytown Housing Association",
            pcsCase.getParties().getFirst().getValue().getPartyFlagsExternal().getPartyName());
    }

    @Test
    void shouldStillUsePersonNameWhenNoOrganisationName() {
        PartyEntity individual = PartyEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Peter")
            .lastName("Parker")
            .build();
        authenticatedUserRepresents(individual);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(individual)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(individual)));
        setClaimParties(pcsCaseEntity, createClaimParty(individual, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertEquals("Peter Parker",
            pcsCase.getPartySupport().getFirst().getValue().getSupportFlags().getPartyName());
    }

    @Test
    void shouldAssociateFlagsByPartyIdRegardlessOfCollectionOrder() {
        CasePartyFlagEntity claimantFlag = createMockCasePartyFlagsEntity();
        claimantFlag.setVisibility("External");
        claimantFlag.setFlagRefData(createMockRefDataFlagsEntity("RA0042", "Reasonable adjustment"));

        PartyEntity claimantEntity = createPartyEntity(null);
        claimantEntity.setDefendantFlags(List.of(claimantFlag));
        PartyEntity defendantEntity = createPartyEntity(null);

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(defendantEntity), mappedParty(claimantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(claimantEntity, defendantEntity)));
        setClaimParties(pcsCaseEntity,
                        createClaimParty(claimantEntity, PartyRole.CLAIMANT),
                        createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mappedDefendant = findPartyById(pcsCase, defendantEntity.getId().toString());
        Party mappedClaimant = findPartyById(pcsCase, claimantEntity.getId().toString());

        assertEquals("Defendant", mappedDefendant.getPartyFlagsExternal().getRoleOnCase());
        assertEquals(0, mappedDefendant.getPartyFlagsExternal().getDetails().size());

        assertEquals("Claimant", mappedClaimant.getPartyFlagsExternal().getRoleOnCase());
        assertEquals(1, mappedClaimant.getPartyFlagsExternal().getDetails().size());
        assertEquals("RA0042",
            mappedClaimant.getPartyFlagsExternal().getDetails().getFirst().getValue().getFlagCode());
    }

    @Test
    void shouldSkipDomainPartiesWithMissingOrMalformedIds() {
        // Given
        PartyEntity defendantEntity = createPartyEntity(null);

        ListValue<Party> noId = ListValue.<Party>builder()
            .value(Party.builder().firstName("No").lastName("Id").build())
            .build();
        ListValue<Party> malformedId = ListValue.<Party>builder()
            .value(Party.builder().id("not-a-uuid").firstName("Bad").lastName("Id").build())
            .build();
        ListValue<Party> noValue = ListValue.<Party>builder().build();

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(noId, malformedId, noValue, mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(defendantEntity)));
        setClaimParties(pcsCaseEntity, createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        assertNull(noId.getValue().getPartyFlagsExternal());
        assertNull(malformedId.getValue().getPartyFlagsExternal());
        assertNotNull(findPartyById(pcsCase, defendantEntity.getId().toString()).getPartyFlagsExternal());
    }

    private PartyEntity partyWithSupport(CasePartyFlagEntity... flags) {
        PartyEntity partyEntity = createPartyEntity(null);
        partyEntity.setDefendantFlags(List.of(flags));

        return partyEntity;
    }

    private PCSCase claimantAndDefendantCase(PartyEntity claimantEntity, PartyEntity defendantEntity) {
        return PCSCase.builder()
            .parties(List.of(mappedParty(claimantEntity), mappedParty(defendantEntity)))
            .build();
    }

    private PcsCaseEntity pcsCaseEntityFor(PartyEntity claimantEntity, PartyEntity defendantEntity) {
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(claimantEntity, defendantEntity)));
        setClaimParties(pcsCaseEntity,
                        createClaimParty(claimantEntity, PartyRole.CLAIMANT),
                        createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        return pcsCaseEntity;
    }

    private void authenticatedUserRepresents(PartyEntity... representedParties) {
        when(securityContextService.getCurrentUserId()).thenReturn(AUTHENTICATED_USER_ID);
        when(partySupportOwnershipResolver.resolveRepresentedPartyIds(any(), eq(AUTHENTICATED_USER_ID)))
            .thenReturn(Arrays.stream(representedParties)
                            .map(PartyEntity::getId)
                            .collect(Collectors.toSet()));
    }

    private Set<String> supportPartyIds(PCSCase pcsCase) {
        return pcsCase.getPartySupport().stream()
            .map(ListValue::getId)
            .collect(Collectors.toSet());
    }

    private Set<String> supportFlagCodes(PCSCase pcsCase) {
        return pcsCase.getPartySupport().stream()
            .flatMap(support -> support.getValue().getSupportFlags().getDetails().stream())
            .map(detail -> detail.getValue().getFlagCode())
            .collect(Collectors.toSet());
    }

    private CasePartyFlagEntity externalFlag(String flagCode, String flagName) {
        CasePartyFlagEntity flag = createMockCasePartyFlagsEntity();
        flag.setVisibility("External");
        flag.setFlagRefData(createMockRefDataFlagsEntity(flagCode, flagName));

        return flag;
    }

    private CasePartyFlagEntity internalFlag(String flagCode, String flagName) {
        CasePartyFlagEntity flag = createMockCasePartyFlagsEntity();
        flag.setVisibility("Internal");
        flag.setFlagRefData(createMockRefDataFlagsEntity(flagCode, flagName));

        return flag;
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
                .id(entity.getId().toString())
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
