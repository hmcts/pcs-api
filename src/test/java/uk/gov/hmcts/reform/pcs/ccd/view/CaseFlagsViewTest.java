package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyId;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    void shouldKeepReviewedSupportFlagsVisibleWhateverTheirStatus() {
        CasePartyFlagEntity notApproved = createMockCasePartyFlagsEntity();
        notApproved.setVisibility("External");
        notApproved.setDefaultStatus("Not approved");
        notApproved.setFlagRefData(createMockRefDataFlagsEntity("RA0042", "Reasonable adjustment"));

        CasePartyFlagEntity inactive = createMockCasePartyFlagsEntity();
        inactive.setVisibility("External");
        inactive.setDefaultStatus("Inactive");
        inactive.setFlagRefData(createMockRefDataFlagsEntity("RA0013", "Assistance dog"));

        PartyEntity defendantEntity = createPartyEntity(null);
        defendantEntity.setDefendantFlags(List.of(notApproved, inactive));

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(Set.of(defendantEntity));
        setClaimParties(pcsCaseEntity, createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mappedDefendant = pcsCase.getParties().getFirst().getValue();

        assertEquals(2, mappedDefendant.getPartyFlagsExternal().getDetails().size());
        assertEquals(List.of("Not approved", "Inactive"),
            mappedDefendant.getPartyFlagsExternal().getDetails().stream()
                .map(detail -> detail.getValue().getStatus())
                .toList());
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
    void shouldMapClaimantSupportWithClaimantRole() {
        CasePartyFlagEntity externalFlag = createMockCasePartyFlagsEntity();
        externalFlag.setVisibility("External");
        externalFlag.setFlagRefData(createMockRefDataFlagsEntity("RA0042", "Reasonable adjustment"));

        PartyEntity claimantEntity = createPartyEntity(null);
        claimantEntity.setDefendantFlags(List.of(externalFlag));

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(claimantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(claimantEntity)));
        setClaimParties(pcsCaseEntity, createClaimParty(claimantEntity, PartyRole.CLAIMANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mappedClaimant = pcsCase.getParties().getFirst().getValue();
        assertNull(mappedClaimant.getPartyFlagsExternal(),
                   "the Case Flags tab is defendants only");
        assertNull(mappedClaimant.getDefendantFlags(),
                   "the Case Flags tab is defendants only");

        Flags claimantSupport = pcsCase.getPartySupport().getFirst().getValue().getSupportFlags();
        assertEquals(1, pcsCase.getPartySupport().size());
        assertEquals("Claimant", claimantSupport.getRoleOnCase());
        assertEquals(1, claimantSupport.getDetails().size());
        assertEquals("RA0042", claimantSupport.getDetails().getFirst().getValue().getFlagCode());
    }

    @Test
    void shouldExposeExternalSupportForEveryPartyRegardlessOfTheAuthenticatedUser() {
        CasePartyFlagEntity claimantExternalFlag = createMockCasePartyFlagsEntity();
        claimantExternalFlag.setVisibility("External");
        claimantExternalFlag.setFlagRefData(createMockRefDataFlagsEntity("RA0042", "Reasonable adjustment"));

        CasePartyFlagEntity defendantExternalFlag = createMockCasePartyFlagsEntity();
        defendantExternalFlag.setVisibility("External");
        defendantExternalFlag.setFlagRefData(createMockRefDataFlagsEntity("RA0033", "Hearing loop"));

        PartyEntity claimantEntity = createPartyEntity(null);
        claimantEntity.setDefendantFlags(List.of(claimantExternalFlag));
        PartyEntity defendantEntity = createPartyEntity(null);
        defendantEntity.setDefendantFlags(List.of(defendantExternalFlag));

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(claimantEntity), mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(claimantEntity, defendantEntity)));
        setClaimParties(pcsCaseEntity,
                        createClaimParty(claimantEntity, PartyRole.CLAIMANT),
                        createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Both sides' external support is present, so the tab stays complete and the count stays correct
        assertEquals(2, pcsCase.getPartySupport().size());
        assertEquals("RA0042", pcsCase.getPartySupport().getFirst()
            .getValue().getSupportFlags().getDetails().getFirst().getValue().getFlagCode());
        assertEquals("RA0033", pcsCase.getPartySupport().get(1)
            .getValue().getSupportFlags().getDetails().getFirst().getValue().getFlagCode());
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

        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(orgClaimant)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(orgClaimant)));
        setClaimParties(pcsCaseEntity, createClaimParty(orgClaimant, PartyRole.CLAIMANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        assertEquals("Anytown Housing Association",
            pcsCase.getPartySupport().getFirst().getValue().getSupportFlags().getPartyName());
    }

    @Test
    void shouldStillUsePersonNameWhenNoOrganisationName() {
        PartyEntity individual = PartyEntity.builder()
            .id(UUID.randomUUID())
            .firstName("Peter")
            .lastName("Parker")
            .build();

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
        CasePartyFlagEntity defendantFlag = createMockCasePartyFlagsEntity();
        defendantFlag.setVisibility("External");
        defendantFlag.setFlagRefData(createMockRefDataFlagsEntity("RA0042", "Reasonable adjustment"));

        PartyEntity claimantEntity = createPartyEntity(null);
        PartyEntity defendantEntity = createPartyEntity(null);
        defendantEntity.setDefendantFlags(List.of(defendantFlag));

        // Domain order and CCD collection order are deliberately reversed, so a positional match
        // would associate the flag with the wrong party.
        PCSCase pcsCase = PCSCase.builder()
            .parties(List.of(mappedParty(claimantEntity), mappedParty(defendantEntity)))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(defendantEntity, claimantEntity)));
        setClaimParties(pcsCaseEntity,
                        createClaimParty(claimantEntity, PartyRole.CLAIMANT),
                        createClaimParty(defendantEntity, PartyRole.DEFENDANT));

        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        Party mappedDefendant = findPartyById(pcsCase, defendantEntity.getId().toString());
        assertEquals("Defendant", mappedDefendant.getPartyFlagsExternal().getRoleOnCase());
        assertEquals(1, mappedDefendant.getPartyFlagsExternal().getDetails().size());
        assertEquals("RA0042",
            mappedDefendant.getPartyFlagsExternal().getDetails().getFirst().getValue().getFlagCode());

        Party mappedClaimant = findPartyById(pcsCase, claimantEntity.getId().toString());
        assertNull(mappedClaimant.getPartyFlagsExternal(), "the Case Flags tab is defendants only");
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

    @Test
    void shouldNotFlagAClaimantOrARepresentingOrganisationOnTheCaseFlagsTab() {
        // Given
        PartyEntity orgClaimant = createPartyEntity("PCS_Local_Auth_Org_Testing");
        orgClaimant.setOrganisationId("HC9X8YK");
        PartyEntity defendant = createPartyEntity(null);

        OrganisationEntity representingOrg = OrganisationEntity.builder()
            .organisationId("YPERQ40")
            .organisationName("PCS_Solicitor_Org_Testing")
            .build();
        defendant.setClaimPartyOrganisationList(new ArrayList<>(List.of(
            ClaimPartyOrganisationEntity.builder()
                .organisation(representingOrg)
                .active(YesOrNo.YES)
                .build())));

        PCSCase pcsCase = PCSCase.builder()
            .parties(new ArrayList<>(List.of(mappedParty(orgClaimant), mappedParty(defendant))))
            .build();
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setParties(new LinkedHashSet<>(List.of(orgClaimant, defendant)));
        setClaimParties(pcsCaseEntity,
                        createClaimParty(orgClaimant, PartyRole.CLAIMANT),
                        createClaimParty(defendant, PartyRole.DEFENDANT));

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        Party projectedClaimant = findPartyById(pcsCase, orgClaimant.getId().toString());
        assertNull(projectedClaimant.getDefendantFlags(),
                   "a claimant must not gain a Case Flags collection");
        assertNull(projectedClaimant.getPartyFlagsExternal(),
                   "a claimant must not gain a Case Flags collection");

        assertNotNull(findPartyById(pcsCase, defendant.getId().toString()).getDefendantFlags(),
                      "defendants remain flaggable");
        assertNotNull(findPartyById(pcsCase, defendant.getId().toString()).getPartyFlagsExternal());

        assertEquals(2, pcsCase.getParties().size(),
                     "the claimant stays a case party and is not removed from the domain");
        assertEquals(orgClaimant.getId().toString(), projectedClaimant.getId(),
                     "the claimant keeps its party id");
        assertEquals(orgClaimant.getId().toString(),
                     findPartyListValueById(pcsCase, orgClaimant.getId()).getId(),
                     "the claimant keeps its collection id");

        assertThat(pcsCase.getPartySupport())
            .as("support still covers the claimant, which is projected separately")
            .anyMatch(listValue -> orgClaimant.getId().toString().equals(listValue.getId()));

        assertThat(pcsCase.getParties())
            .as("the representing organisation must never be projected as a party of its own")
            .noneMatch(listValue -> "PCS_Solicitor_Org_Testing".equals(listValue.getValue().getOrgName()));
        assertThat(pcsCase.getPartySupport())
            .as("representation organisations are not support parties either")
            .noneMatch(listValue -> "YPERQ40".equals(listValue.getId()));
    }

    private ListValue<Party> findPartyListValueById(PCSCase pcsCase, UUID partyId) {
        return pcsCase.getParties().stream()
            .filter(listValue -> partyId.toString().equals(listValue.getValue().getId()))
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
