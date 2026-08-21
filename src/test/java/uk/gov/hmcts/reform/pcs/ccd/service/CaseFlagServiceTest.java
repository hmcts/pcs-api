package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.camunda.CamundaService;
import uk.gov.hmcts.reform.pcs.camunda.TaskType;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartySupportOwnershipResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

//import static org.assertj.core.api.Assertions.assertE;

@ExtendWith(MockitoExtension.class)
class CaseFlagServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private FlagRefDataRepository flagRefDataRepository;
    @Mock
    private CamundaService camundaService;
    @Mock
    private TaskDescriptionService taskDescriptionService;
    @Mock
    private PartySupportOwnershipResolver partySupportOwnershipResolver;

    @Mock
    private TranslationWAService translationWAService;

    @InjectMocks
    private CaseFlagService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseFlagService(flagRefDataRepository, camundaService, taskDescriptionService,
                                        partySupportOwnershipResolver, translationWAService);
    }

    @Test
    void shouldMergeNewCaseFlags() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null,"CF0002", "Complex Case",
                                      "Complicated case", "Active"))
            .build();

        // When
        List<CaseFlagEntity> savedFlags = underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        // Then
        assertThat(savedFlags.getFirst().getFlagRefData().getFlagCode()).isEqualTo("CF0002");
        assertThat(savedFlags.getFirst().getFlagComment()).isEqualTo("Complicated case");
        assertThat(savedFlags.getFirst().getDefaultStatus()).isEqualTo("Active");
        assertThat(savedFlags).hasSize(1);

        String savedPaths = Arrays.stream(savedFlags.getFirst().getPaths().split(":")).toList().getLast();
        assertNotNull(savedPaths);
        assertThat(savedPaths).isEqualTo("Case");
    }

    @Test
    void shouldMergeNewCaseFlagsWithPaths() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null,"CF0007", "Urgent case",
                                      "Urgent case test", "Active"))
            .build();

        // When
        List<CaseFlagEntity> savedFlags = underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        // Then
        String savedPaths = savedFlags.getFirst().getPaths();
        assertThat(savedFlags.getFirst().getDefaultStatus()).isEqualTo("Active");
        assertThat(savedFlags).hasSize(1);
        assertThat(savedPaths).contains("Case");
    }

    @Test
    void shouldAmendExistingCaseFlags() {
        // Given
        UUID id = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity(id);
        List<ListValue<FlagDetail>> flagDetails = new ArrayList<>(createFlagDetail(
            id.toString(), "CF0008", "Power of arrest with Police ",
            "Police arrest inactive", "Inactive"
        ));
        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(flagDetails)
            .build();

        // When
        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        // Then
        assertNotNull(pcsCaseEntity.getCaseFlags());
        List<CaseFlagEntity> savedFlags = pcsCaseEntity.getCaseFlags();
        assertThat(savedFlags).hasSize(1);
        assertThat(savedFlags.getLast().getFlagComment()).isEqualTo("Police arrest inactive");
        assertThat(savedFlags.getLast().getFlagRefData().getFlagCode()).isEqualTo("CF0008");
    }

    @Test
    void testMergePartyFlags_NewPartyWithFlags() {
        UUID partyId = UUID.randomUUID();
        Set<PartyEntity> partyEntities = createPartyEntities(partyId);
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().parties(partyEntities).build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null,"CF0002", "Complex Case",
                                      "Complicated case", "Active"))
            .build();

        Party incomingParty = Party.builder().defendantFlags(incomingFlags).build();
        List<ListValue<Party>> parties = List.of(createPartyListValue(partyId.toString(), incomingParty));

        underTest.mergePartyFlags(parties, pcsCaseEntity.getParties());

        assertThat(pcsCaseEntity.getParties()).hasSize(1);
        PartyEntity savedParty = pcsCaseEntity.getParties().iterator().next();

        assertNotNull(savedParty.getDefendantFlags());
        assertThat(savedParty.getDefendantFlags()).hasSize(1);

        BaseCaseFlag savedFlags = savedParty.getDefendantFlags().getFirst();
        assertThat(savedFlags.getFlagComment()).isEqualTo("Complicated case");
        assertThat(savedFlags.getDefaultStatus()).isEqualTo("Active");

    }

    @Test
    void shouldReplaceOnlyReasonableAdjustmentFlagsOnParty() {
        // Given
        List<CasePartyFlagEntity> existingFlags = new ArrayList<>();
        existingFlags.add(createPartyFlagEntity("RA0012", "Braille documents"));
        existingFlags.add(createPartyFlagEntity("PF0015", "Language Interpreter"));

        PartyEntity partyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .defendantFlags(existingFlags)
            .build();

        Flags incomingFlags = Flags.builder()
            .partyName("Jack Smith")
            .roleOnCase("Defendant")
            .details(createFlagDetailsWithoutIds("RA0033", "Sign language interpreter"))
            .build();

        when(taskDescriptionService.createReviewCaseFlagDescription(eq(CASE_REFERENCE), any()))
            .thenReturn("description");

        // When
        underTest.saveReasonableAdjustmentFlags(partyEntity, incomingFlags, CASE_REFERENCE);

        // Then
        assertThat(partyEntity.getDefendantFlags())
            .extracting(flag -> flag.getFlagRefData().getFlagCode())
            .containsExactlyInAnyOrder("PF0015", "RA0033");

        verify(camundaService).createTask(CASE_REFERENCE, TaskType.REVIEW_CASE_FLAG, "description");
    }

    @Test
    void shouldIgnoreSuppliedFlagsThatAreNotReasonableAdjustments() {
        // Given
        List<CasePartyFlagEntity> existingFlags = new ArrayList<>();
        existingFlags.add(createPartyFlagEntity("PF0015", "Language Interpreter"));

        when(taskDescriptionService.createReviewCaseFlagDescription(eq(CASE_REFERENCE), any()))
            .thenReturn("description");

        PartyEntity partyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .defendantFlags(existingFlags)
            .build();

        List<ListValue<FlagDetail>> details = new ArrayList<>();
        details.addAll(createFlagDetailsWithoutIds("RA0033", "Sign language interpreter"));
        details.addAll(createFlagDetailsWithoutIds("CF0002", "Complex case"));

        // When
        underTest.saveReasonableAdjustmentFlags(partyEntity, Flags.builder().details(details).build(), CASE_REFERENCE);

        // Then
        assertThat(partyEntity.getDefendantFlags())
            .extracting(flag -> flag.getFlagRefData().getFlagCode())
            .containsExactlyInAnyOrder("PF0015", "RA0033");

        verify(camundaService).createTask(CASE_REFERENCE, TaskType.REVIEW_CASE_FLAG, "description");
    }

    @Test
    void shouldRetainExistingFlagsWhenNoReasonableAdjustmentsSupplied() {
        // Given
        List<CasePartyFlagEntity> existingFlags = new ArrayList<>();
        existingFlags.add(createPartyFlagEntity("RA0012", "Braille documents"));

        PartyEntity partyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .defendantFlags(existingFlags)
            .build();

        Flags incomingFlags = Flags.builder()
            .details(createFlagDetailsWithoutIds("CF0002", "Complex case"))
            .build();

        // When
        underTest.saveReasonableAdjustmentFlags(partyEntity, incomingFlags, CASE_REFERENCE);

        // Then
        assertThat(partyEntity.getDefendantFlags())
            .extracting(flag -> flag.getFlagRefData().getFlagCode())
            .containsExactly("RA0012");
        // The non reasonable adjustment flag is dropped before any reference data is touched
        verifyNoInteractions(flagRefDataRepository);
        verifyNoInteractions(taskDescriptionService);
        verifyNoInteractions(camundaService);
    }

    @Test
    void shouldNotRewriteSharedReferenceDataFromASuppliedFlag() {
        // Given reference data already describing this flag code
        FlagRefDataEntity existingRefData = FlagRefDataEntity.builder()
            .flagCode("RA0035")
            .flagName("Video hearing")
            .flagNameWelsh("Gwrandawiad fideo")
            .hearingRelevant(true)
            .availableExternally(true)
            .visibility(FlagVisibility.INTERNAL.getValue())
            .build();
        when(flagRefDataRepository.findByFlagCode("RA0035")).thenReturn(Optional.of(existingRefData));

        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).build();

        // and a payload claiming something different about it
        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.EXTERNAL)
            .details(List.of(ListValue.<FlagDetail>builder()
                                 .value(FlagDetail.builder()
                                            .flagCode("RA0035")
                                            .name("Overwritten name")
                                            .nameCy("Overwritten welsh name")
                                            .status("Requested")
                                            .hearingRelevant(YesOrNo.NO)
                                            .availableExternally(YesOrNo.NO)
                                            .build())
                                 .build()))
            .build();

        // When
        underTest.saveReasonableAdjustmentFlags(partyEntity, incomingFlags, CASE_REFERENCE);

        // Then the flag is stored against the party, but the shared reference data is untouched
        assertThat(partyEntity.getDefendantFlags()).hasSize(1);
        assertThat(partyEntity.getDefendantFlags().getFirst().getFlagRefData()).isSameAs(existingRefData);
        assertThat(existingRefData.getFlagName()).isEqualTo("Video hearing");
        assertThat(existingRefData.getFlagNameWelsh()).isEqualTo("Gwrandawiad fideo");
        assertThat(existingRefData.getHearingRelevant()).isTrue();
        assertThat(existingRefData.getAvailableExternally()).isTrue();
        assertThat(existingRefData.getVisibility()).isEqualTo(FlagVisibility.INTERNAL.getValue());

        verifyNoInteractions(taskDescriptionService);
        verifyNoInteractions(camundaService);
    }

    @Test
    void shouldCreateReferenceDataForAnUnseenSuppliedFlagCode() {
        // Given
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).build();

        Flags incomingFlags = Flags.builder()
            .details(createFlagDetailsWithoutIds("RA0099", "Newly published adjustment"))
            .build();

        when(taskDescriptionService.createReviewCaseFlagDescription(eq(CASE_REFERENCE), any()))
            .thenReturn("description");

        // When
        underTest.saveReasonableAdjustmentFlags(partyEntity, incomingFlags, CASE_REFERENCE);

        // Then
        FlagRefDataEntity createdRefData = partyEntity.getDefendantFlags().getFirst().getFlagRefData();
        assertThat(createdRefData.getFlagCode()).isEqualTo("RA0099");
        assertThat(createdRefData.getFlagName()).isEqualTo("Newly published adjustment");

        verify(camundaService).createTask(CASE_REFERENCE, TaskType.REVIEW_CASE_FLAG, "description");
    }

    @Test
    void shouldUpdateSharedReferenceDataFromCaseworkerFlags() {
        // Given reference data a caseworker is correcting
        FlagRefDataEntity existingRefData = FlagRefDataEntity.builder()
            .flagCode("CF0002")
            .flagName("Complex Case")
            .build();
        when(flagRefDataRepository.findByFlagCode("CF0002")).thenReturn(Optional.of(existingRefData));

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null, "CF0002", "Complex case - renamed",
                                      "Complicated case", "Active"))
            .build();

        // When
        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        // Then
        assertThat(existingRefData.getFlagName()).isEqualTo("Complex case - renamed");
    }

    @Test
    void shouldStorePathsWhenPathValuesHaveNoIds() {
        // Given
        PartyEntity partyEntity = PartyEntity.builder().id(UUID.randomUUID()).build();

        Flags incomingFlags = Flags.builder()
            .details(createFlagDetailsWithoutIds("RA0012", "Braille documents"))
            .build();

        when(taskDescriptionService.createReviewCaseFlagDescription(eq(CASE_REFERENCE), any()))
            .thenReturn("description");

        // When
        underTest.saveReasonableAdjustmentFlags(partyEntity, incomingFlags, CASE_REFERENCE);

        // Then
        assertThat(partyEntity.getDefendantFlags().getFirst().getPaths())
            .isEqualTo(":Party_:Reasonable adjustment");

        verify(camundaService).createTask(CASE_REFERENCE, TaskType.REVIEW_CASE_FLAG, "description");
    }

    @Test
    void shouldRetainExistingPartyFlagsWhenNoFlagsSupplied() {
        // Given
        List<CasePartyFlagEntity> existingFlags = new ArrayList<>();
        existingFlags.add(createPartyFlagEntity("RA0012", "Braille documents"));

        PartyEntity partyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .defendantFlags(existingFlags)
            .build();

        // When
        underTest.saveReasonableAdjustmentFlags(partyEntity, null, CASE_REFERENCE);
        underTest.saveReasonableAdjustmentFlags(
            partyEntity, Flags.builder().details(new ArrayList<>()).build(), CASE_REFERENCE
        );

        // Then
        assertThat(partyEntity.getDefendantFlags()).hasSize(1);
        assertThat(partyEntity.getDefendantFlags().getFirst().getFlagComment()).isEqualTo("Braille documents");
        // The method returns before doing anything else - no reference data is looked up or written
        verifyNoInteractions(flagRefDataRepository);
        verifyNoInteractions(taskDescriptionService);
        verifyNoInteractions(camundaService);
    }

    private List<ListValue<FlagDetail>> createFlagDetailsWithoutIds(String flagCode, String name) {
        return List.of(ListValue.<FlagDetail>builder()
                           .value(FlagDetail.builder()
                                      .flagCode(flagCode)
                                      .name(name)
                                      .status("Active")
                                      .hearingRelevant(YesOrNo.YES)
                                      .availableExternally(YesOrNo.YES)
                                      .dateTimeCreated(LocalDateTime.now())
                                      .path(List.of(
                                          ListValue.<String>builder().value("Party").build(),
                                          ListValue.<String>builder().value("Reasonable adjustment").build()))
                                      .build())
                           .build());
    }

    private CasePartyFlagEntity createPartyFlagEntity(String flagCode, String flagComment) {
        CasePartyFlagEntity casePartyFlagEntity = new CasePartyFlagEntity();
        casePartyFlagEntity.setId(UUID.randomUUID());
        casePartyFlagEntity.setDefaultStatus("Active");
        casePartyFlagEntity.setFlagRefData(FlagRefDataEntity.builder().flagCode(flagCode).build());
        casePartyFlagEntity.setFlagComment(flagComment);
        casePartyFlagEntity.setPaths(":Party");

        return casePartyFlagEntity;
    }

    private Set<PartyEntity> createPartyEntities(UUID partyId) {
        Set<PartyEntity> parties = new HashSet<>();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .firstName("King")
            .lastName("Smith")
            .build();
        parties.add(partyEntity);
        return parties;
    }

    @Test
    void testMergePartyFlags_UpdateExistingPartyFlags() {
        // Given
        UUID existingPartyId = UUID.randomUUID();

        CasePartyFlagEntity existingPartyFlagsEntityFirst = createCasePartyFlagEntity(
            existingPartyId, "Active", "Spanish Language Interpreter");
        CasePartyFlagEntity existingPartyFlagsEntitySecond = createCasePartyFlagEntity(
            UUID.randomUUID(), "Inactive", "German Language Interpreter");

        List<CasePartyFlagEntity> casePartyFlagEntities = new ArrayList<>();
        casePartyFlagEntities.add(existingPartyFlagsEntityFirst);
        casePartyFlagEntities.add(existingPartyFlagsEntitySecond);

        PartyEntity existingParty = PartyEntity.builder()
            .id(existingPartyId)
            .defendantFlags(casePartyFlagEntities)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(new HashSet<>(List.of(existingParty)))
            .build();

        Flags updatedFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details((createFlagDetail(existingPartyId.toString(),"PF00015", "Language Interpreter ",
                                       "Spanish Language Interpreter inactive", "Inactive")))
            .build();

        Party incomingParty = Party.builder().defendantFlags(updatedFlags).build();
        List<ListValue<Party>> incomingParties = List.of(createPartyListValue(
            existingPartyId.toString(),
            incomingParty
        ));

        // When
        underTest.mergePartyFlags(incomingParties, pcsCaseEntity.getParties());

        // Then
        assertThat(pcsCaseEntity.getParties()).hasSize(1);
        PartyEntity updatedParty = pcsCaseEntity.getParties().iterator().next();

        assertNotNull(updatedParty.getDefendantFlags());
        assertThat(updatedParty.getDefendantFlags()).hasSize(1);
        assertThat(updatedParty.getDefendantFlags().getLast().getFlagComment()).isEqualTo(
            "Spanish Language Interpreter inactive");

    }

    @Test
    void shouldTriggerTranslationTasksWhenWelshCommunicationsFlagBecomesActive() {
        // Given
        UUID partyId = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(1234L).build();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(pcsCaseEntity)
            .build();
        pcsCaseEntity.setParties(new HashSet<>(List.of(partyEntity)));

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null, "PF0026",
                "I want to receive communications and documents in Welsh", "Welsh comms", "Active"))
            .build();

        Party incomingParty = Party.builder().defendantFlags(incomingFlags).build();
        List<ListValue<Party>> parties = List.of(createPartyListValue(partyId.toString(), incomingParty));

        // When
        underTest.mergePartyFlags(parties, pcsCaseEntity.getParties());

        // Then
        verify(translationWAService).triggerTranslationTasksForFlaggingParty(partyEntity);
    }

    @Test
    void shouldNotCreateTranslationTaskWhenFlagCodeDoesNotMatchWelshCommunications() {
        // Given
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(PcsCaseEntity.builder().caseReference(1234L).build())
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(new HashSet<>(List.of(partyEntity)))
            .build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null, "PF00015", "Language Interpreter",
                                      "Spanish Language Interpreter", "Active"))
            .build();

        Party incomingParty = Party.builder().defendantFlags(incomingFlags).build();
        List<ListValue<Party>> parties = List.of(createPartyListValue(partyId.toString(), incomingParty));

        // When
        underTest.mergePartyFlags(parties, pcsCaseEntity.getParties());

        // Then
        verifyNoInteractions(translationWAService);
    }

    @Test
    void shouldNotTriggerAnyTranslationTaskWhenFlagIsInactive() {
        // Given
        UUID partyId = UUID.randomUUID();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(PcsCaseEntity.builder().caseReference(1234L).build())
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(new HashSet<>(List.of(partyEntity)))
            .build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null, "PF0026",
                "I want to receive communications and documents in Welsh", "Welsh comms", "Inactive"))
            .build();

        Party incomingParty = Party.builder().defendantFlags(incomingFlags).build();
        List<ListValue<Party>> parties = List.of(createPartyListValue(partyId.toString(), incomingParty));

        // When
        underTest.mergePartyFlags(parties, pcsCaseEntity.getParties());

        // Then
        verifyNoInteractions(translationWAService);
    }

    @Test
    void shouldNotTriggerAnyTranslationTaskWhenFlagAlreadyActive() {
        // Given
        UUID partyId = UUID.randomUUID();
        UUID otherDefendantId = UUID.randomUUID();

        CasePartyFlagEntity existingWelshFlag = new CasePartyFlagEntity();
        existingWelshFlag.setId(UUID.randomUUID());
        existingWelshFlag.setDefaultStatus("Active");
        existingWelshFlag.setFlagRefData(FlagRefDataEntity.builder().flagCode("PF0026").build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().caseReference(1234L).build();
        PartyEntity partyEntity = PartyEntity.builder()
            .id(partyId)
            .pcsCase(pcsCaseEntity)
            .defendantFlags(new ArrayList<>(List.of(existingWelshFlag)))
            .build();
        PartyEntity otherDefendant = PartyEntity.builder().id(otherDefendantId).pcsCase(pcsCaseEntity).build();
        pcsCaseEntity.setParties(new HashSet<>(List.of(partyEntity, otherDefendant)));

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(existingWelshFlag.getId().toString(), "PF0026",
                "I want to receive communications and documents in Welsh", "Welsh comms", "Active"))
            .build();

        Party incomingParty = Party.builder().defendantFlags(incomingFlags).build();
        List<ListValue<Party>> parties = List.of(createPartyListValue(partyId.toString(), incomingParty));

        // When
        underTest.mergePartyFlags(parties, pcsCaseEntity.getParties());

        // Then
        verifyNoInteractions(translationWAService);
    }

    @Test
    void shouldRecordCaseFlagsAsInternal() {
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        Flags incomingFlags = Flags.builder()
            .details(createFlagDetail(null, "CF0002", "Complex Case", "Complicated case", "Requested"))
            .build();

        List<CaseFlagEntity> savedFlags = underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertThat(savedFlags.getFirst().getVisibility()).isEqualTo("Internal");
        assertThat(savedFlags.getFirst().getDefaultStatus()).isEqualTo("Requested");
    }

    @Test
    void shouldMergeInternalAndExternalPartyFlagsIntoOneCollection() {
        UUID partyId = UUID.randomUUID();
        Set<PartyEntity> partyEntities = createPartyEntities(partyId);

        Party incomingParty = Party.builder()
            .defendantFlags(Flags.builder()
                                .visibility(FlagVisibility.INTERNAL)
                                .details(createFlagDetail(null, "PF0002", "Vulnerable user",
                                                          "Internal only flag", "Active"))
                                .build())
            .partyFlagsExternal(Flags.builder()
                                        .visibility(FlagVisibility.EXTERNAL)
                                        .details(createFlagDetail(null, "PF0015", "Language Interpreter",
                                                                  "Externally visible flag", "Requested"))
                                        .build())
            .build();

        underTest.mergePartyFlags(List.of(createPartyListValue(partyId.toString(), incomingParty)), partyEntities);

        List<CasePartyFlagEntity> savedFlags = partyEntities.iterator().next().getDefendantFlags();
        assertThat(savedFlags).hasSize(2);
        assertThat(savedFlags)
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactlyInAnyOrder(
                tuple("Internal only flag", "Internal"),
                tuple("Externally visible flag", "External"));
    }

    @Test
    void shouldRetainExternalPartyFlagsWhenOnlyInternalFlagsSubmitted() {
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity existingExternalFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Existing external flag");
        existingExternalFlag.setVisibility("External");

        PartyEntity existingParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(existingExternalFlag)))
            .build();

        Party incomingParty = Party.builder()
            .defendantFlags(Flags.builder()
                                .visibility(FlagVisibility.INTERNAL)
                                .details(createFlagDetail(null, "PF0002", "Vulnerable user",
                                                          "New internal flag", "Active"))
                                .build())
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactlyInAnyOrder(
                tuple("New internal flag", "Internal"),
                tuple("Existing external flag", "External"));
    }

    @Test
    void shouldRetainInternalPartyFlagsWhenOnlyExternalFlagsSubmitted() {
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity existingInternalFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Existing internal flag");
        existingInternalFlag.setVisibility("Internal");

        PartyEntity existingParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(existingInternalFlag)))
            .build();

        Party incomingParty = Party.builder()
            .partyFlagsExternal(Flags.builder()
                                        .visibility(FlagVisibility.EXTERNAL)
                                        .details(createFlagDetail(null, "PF0015", "Language Interpreter",
                                                                  "New external flag", "Requested"))
                                        .build())
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactlyInAnyOrder(
                tuple("Existing internal flag", "Internal"),
                tuple("New external flag", "External"));
    }

    @Test
    void shouldRetainPartyFlagsWhenIncomingPartyHasNoFlagCollections() {
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Existing internal flag");
        existingFlag.setVisibility("Internal");

        PartyEntity existingParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(existingFlag)))
            .build();

        Party incomingParty = Party.builder().build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags()).containsExactly(existingFlag);
        verifyNoInteractions(flagRefDataRepository);
    }

    @Test
    void shouldRetainPartyFlagsWhenIncomingFlagCollectionsAreEmpty() {
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Existing external flag");
        existingFlag.setVisibility("External");

        PartyEntity existingParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(existingFlag)))
            .build();

        Party incomingParty = Party.builder()
            .defendantFlags(Flags.builder().visibility(FlagVisibility.INTERNAL).details(List.of()).build())
            .partyFlagsExternal(Flags.builder().visibility(FlagVisibility.EXTERNAL).details(List.of()).build())
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags()).containsExactly(existingFlag);
        verifyNoInteractions(flagRefDataRepository);
    }

    @Test
    void testMergePartyFlags_NoIncomingChanges() {
        PartyEntity existingParty = PartyEntity.builder()
            .id(UUID.randomUUID())
            .firstName("John")
            .lastName("Doe")
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(new HashSet<>(List.of(existingParty)))
            .build();

        underTest.mergePartyFlags(new ArrayList<>(), pcsCaseEntity.getParties());

        assertThat(pcsCaseEntity.getParties()).hasSize(1);
        PartyEntity retainedParty = pcsCaseEntity.getParties().iterator().next();

        assertThat(retainedParty.getFirstName()).isEqualTo("John");
        assertThat(retainedParty.getLastName()).isEqualTo("Doe");
        assertTrue(retainedParty.getDefendantFlags().isEmpty());
    }

    private PcsCaseEntity createPcsCaseEntity(UUID id) {
        return PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseReference(1234L)
            .caseFlags(createCaseFlagEntity(id))
            .build();
    }

    private List<ListValue<FlagDetail>> createFlagDetail(String id, String flagCode, String name,
                                                         String flagComment, String status) {
        List<ListValue<FlagDetail>> flagDetails = new ArrayList<>();

        ListValue<FlagDetail> flagDetailListValue = ListValue.<FlagDetail>builder()
            .id(id == null ? UUID.randomUUID().toString() : id)
            .value(FlagDetail.builder()
                       .flagCode(flagCode)
                       .name(name)
                       .flagComment(flagComment)
                       .status(status)
                       .availableExternally(YesOrNo.NO)
                       .hearingRelevant(YesOrNo.YES)
                       .path(createPathListValue())
                       .build())
            .build();
        flagDetails.add(flagDetailListValue);

        return flagDetails;
    }

    private List<ListValue<String>> createPathListValue() {
        List<ListValue<String>> paths = new ArrayList<>();

        ListValue<String> path = ListValue.<String>builder()
            .id(UUID.randomUUID().toString())
            .value("Case")
            .build();
        paths.add(path);

        return paths;
    }

    private List<CaseFlagEntity> createCaseFlagEntity(UUID id) {


        FlagRefDataEntity flagRefDataEntity = new FlagRefDataEntity();
        CaseFlagEntity caseFlagEntity = new CaseFlagEntity();
        caseFlagEntity.setFlagRefData(flagRefDataEntity);
        caseFlagEntity.setId(id);
        caseFlagEntity.setDefaultStatus("Active");
        caseFlagEntity.getFlagRefData().setFlagCode("CF0008");
        caseFlagEntity.setFlagComment("Police arrest inactive");
        caseFlagEntity.setPaths("Case");
        caseFlagEntity.setDateTimeModified(LocalDateTime.now());

        List<CaseFlagEntity> caseFlagEntities = new ArrayList<>();
        caseFlagEntities.add(caseFlagEntity);

        return caseFlagEntities;
    }

    private CasePartyFlagEntity createCasePartyFlagEntity(UUID id, String status, String flagComment) {


        CasePartyFlagEntity casePartyFlagEntity = new CasePartyFlagEntity();
        casePartyFlagEntity.setId(id);
        casePartyFlagEntity.setDefaultStatus(status);
        casePartyFlagEntity.setFlagRefData(FlagRefDataEntity.builder().flagCode("PF00015").build());
        casePartyFlagEntity.setFlagComment(flagComment);
        casePartyFlagEntity.setPaths("Party");
        casePartyFlagEntity.setDateTimeModified(LocalDateTime.now());

        return casePartyFlagEntity;
    }

    private ListValue<Party> createPartyListValue(String id, Party party) {
        return ListValue.<Party>builder()
            .id(id)
            .value(party)
            .build();
    }

    @Test
    void shouldMergePartySupportFlagsAndRetainInternalFlags() {
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity existingInternalFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Existing internal flag");
        existingInternalFlag.setVisibility("Internal");

        PartyEntity existingParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(existingInternalFlag)))
            .build();

        PartySupport incomingSupport = PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(createFlagDetail(null, "RA0042", "Reasonable adjustment",
                                                        "New support request", "Requested"))
                              .build())
            .build();

        underTest.mergePartySupportFlags(
            List.of(createPartySupportListValue(partyId.toString(), incomingSupport)),
            Set.of(existingParty), USER_ID, false);

        assertThat(existingParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactlyInAnyOrder(
                tuple("Existing internal flag", "Internal"),
                tuple("New support request", "External"));
    }

    @Test
    void shouldRejectPartySupportForPartyNotOnTheCase() {
        PartyEntity existingParty = PartyEntity.builder()
            .id(UUID.randomUUID())
            .defendantFlags(new ArrayList<>())
            .build();

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(UUID.randomUUID().toString(), supportRequest()));
        Set<PartyEntity> existingParties = Set.of(existingParty);

        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID, false));

        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("Support submitted for a party that is not on this case");
        assertThat(existingParty.getDefendantFlags()).isEmpty();
        verifyNoInteractions(flagRefDataRepository);
    }

    @Test
    void shouldRejectPartySupportWithMalformedPartyId() {
        PartyEntity existingParty = PartyEntity.builder()
            .id(UUID.randomUUID())
            .defendantFlags(new ArrayList<>())
            .build();

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue("not-a-uuid", supportRequest()));
        Set<PartyEntity> existingParties = Set.of(existingParty);

        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID, false));

        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("Support submitted for an invalid party reference");
        verifyNoInteractions(flagRefDataRepository);
    }

    @Disabled
    @Test
    void shouldAllowOwnSideRequestSupportWhenOtherSideEntriesAreUnchanged() {
        // Given
        UUID ownPartyId = UUID.randomUUID();
        UUID otherPartyId = UUID.randomUUID();

        CasePartyFlagEntity otherSideFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Other side existing support");
        otherSideFlag.setVisibility("External");

        CasePartyFlagEntity otherSideInternalFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Other side internal flag");
        otherSideInternalFlag.setVisibility("Internal");

        PartyEntity ownParty = PartyEntity.builder()
            .id(ownPartyId)
            .defendantFlags(new ArrayList<>())
            .build();
        PartyEntity otherParty = PartyEntity.builder()
            .id(otherPartyId)
            .defendantFlags(new ArrayList<>(List.of(otherSideFlag, otherSideInternalFlag)))
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(ownParty, USER_ID)).thenReturn(true);
        when(partySupportOwnershipResolver.isOwnedByUser(otherParty, USER_ID)).thenReturn(false);

        // When
        underTest.mergePartySupportFlags(
            List.of(createPartySupportListValue(ownPartyId.toString(), supportRequest()),
                    createPartySupportListValue(otherPartyId.toString(), unchangedSupport(otherSideFlag))),
            new HashSet<>(List.of(ownParty, otherParty)), USER_ID, true);

        // Then
        assertThat(ownParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment)
            .containsExactly("New support request");
        assertThat(otherParty.getDefendantFlags())
            .containsExactlyInAnyOrder(otherSideFlag, otherSideInternalFlag);
    }

    @Disabled
    @Test
    void shouldRejectCrossSideChangeToAnExistingSupportFlag() {
        // Given
        UUID otherPartyId = UUID.randomUUID();
        CasePartyFlagEntity otherSideFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Other side existing support");
        otherSideFlag.setVisibility("External");

        PartyEntity otherParty = PartyEntity.builder()
            .id(otherPartyId)
            .defendantFlags(new ArrayList<>(List.of(otherSideFlag)))
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(otherParty, USER_ID)).thenReturn(false);

        PartySupport mutatedSupport = PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(List.of(ListValue.<FlagDetail>builder()
                                  .id(otherSideFlag.getId().toString())
                                  .value(FlagDetail.builder()
                                      .status("Inactive")
                                      .flagComment(otherSideFlag.getFlagComment())
                                      .flagUpdateComment("Withdrawn by the other side")
                                      .build())
                                  .build()))
                              .build())
            .build();

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(otherPartyId.toString(), mutatedSupport));
        Set<PartyEntity> existingParties = Set.of(otherParty);

        // When
        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID, true));

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(otherParty.getDefendantFlags()).containsExactly(otherSideFlag);
        assertThat(otherSideFlag.getDefaultStatus()).isEqualTo("Active");
    }

    @Test
    void shouldRetainSupportWhenIncomingPartySupportValueIsNull() {
        // Given
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Existing support");
        existingFlag.setVisibility("External");

        PartyEntity existingParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(existingFlag)))
            .build();

        // When
        underTest.mergePartySupportFlags(
            List.of(createPartySupportListValue(partyId.toString(), null)),
            Set.of(existingParty), USER_ID, false);

        // Then
        assertThat(existingParty.getDefendantFlags()).containsExactly(existingFlag);
        verifyNoInteractions(flagRefDataRepository);
    }

    @Disabled
    @Test
    void shouldRejectManageSupportForAnotherPartysSupportHasExistingFlags() {
        UUID partyId = UUID.randomUUID();
        PartyEntity otherSideParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(existingExternalFlag())))
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(otherSideParty, USER_ID)).thenReturn(false);

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(partyId.toString(), supportRequest()));
        Set<PartyEntity> existingParties = Set.of(otherSideParty);

        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID, true));

        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(otherSideParty.getDefendantFlags()).isNotEmpty();
        verifyNoInteractions(flagRefDataRepository);
    }

    @Test
    void shouldAllowManageSupportForOwnParty() {
        UUID partyId = UUID.randomUUID();
        PartyEntity ownParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>())
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(ownParty, USER_ID)).thenReturn(true);

        underTest.mergePartySupportFlags(
            List.of(createPartySupportListValue(partyId.toString(), supportRequest())),
            Set.of(ownParty), USER_ID, true);

        assertThat(ownParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactly(tuple("New support request", "External"));
    }

    @Test
    void shouldAllowMergePartySupportFlagsWhenWithNoExistingFlagsWhenNotOwnParty() {
        UUID partyId = UUID.randomUUID();
        PartyEntity ownParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>())
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(ownParty, USER_ID)).thenReturn(false);

        underTest.mergePartySupportFlags(
            List.of(createPartySupportListValue(partyId.toString(), supportRequest())),
            Set.of(ownParty), USER_ID, true);

        assertThat(ownParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactly(tuple("New support request", "External"));
    }

    private PartySupport unchangedSupport(CasePartyFlagEntity existingFlag) {
        return echoedSupport(existingFlag.getId().toString(),
                             FlagDetail.builder()
                                 .status(existingFlag.getDefaultStatus())
                                 .flagComment(existingFlag.getFlagComment())
                                 .flagUpdateComment(existingFlag.getFlagUpdateComment())
                                 .build());
    }

    private PartySupport echoedSupport(String detailId, FlagDetail flagDetail) {
        return PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(List.of(ListValue.<FlagDetail>builder()
                                  .id(detailId)
                                  .value(flagDetail)
                                  .build()))
                              .build())
            .build();
    }

    private Throwable crossSideSubmission(CasePartyFlagEntity existingFlag, PartySupport incomingSupport) {
        PartyEntity otherParty = PartyEntity.builder()
            .id(UUID.randomUUID())
            .defendantFlags(new ArrayList<>(List.of(existingFlag)))
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(otherParty, USER_ID)).thenReturn(false);

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(otherParty.getId().toString(), incomingSupport));
        Set<PartyEntity> existingParties = Set.of(otherParty);

        return catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID, true));
    }

    private CasePartyFlagEntity existingExternalFlag() {
        CasePartyFlagEntity existingFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Other side existing support");
        existingFlag.setVisibility("External");
        existingFlag.setFlagUpdateComment("Original update comment");
        return existingFlag;
    }

    @Disabled
    @Test
    void shouldRejectCrossSideChangeToTheSupportComment() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport commentChanged = echoedSupport(existingFlag.getId().toString(),
                                                    FlagDetail.builder()
                                                        .status(existingFlag.getDefaultStatus())
                                                        .flagComment("Reworded by the other side")
                                                        .flagUpdateComment(existingFlag.getFlagUpdateComment())
                                                        .build());

        // When
        Throwable throwable = crossSideSubmission(existingFlag, commentChanged);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Disabled
    @Test
    void shouldRejectCrossSideChangeToTheSupportUpdateCommentOnly() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport updateCommentChanged = echoedSupport(existingFlag.getId().toString(),
                                                          FlagDetail.builder()
                                                              .status(existingFlag.getDefaultStatus())
                                                              .flagComment(existingFlag.getFlagComment())
                                                              .flagUpdateComment("Amended by the other side")
                                                              .build());

        // When
        Throwable throwable = crossSideSubmission(existingFlag, updateCommentChanged);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Disabled
    @Test
    void shouldRejectCrossSideSubmissionReferencingAnUnknownSupportFlagId() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport unknownFlagId = echoedSupport(UUID.randomUUID().toString(),
                                                   FlagDetail.builder()
                                                       .status(existingFlag.getDefaultStatus())
                                                       .flagComment(existingFlag.getFlagComment())
                                                       .flagUpdateComment(existingFlag.getFlagUpdateComment())
                                                       .build());

        // When
        Throwable throwable = crossSideSubmission(existingFlag, unknownFlagId);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Disabled("Logic issue in production code")
    @Test
    void shouldRejectCrossSideSubmissionThatStripsTheOtherSidesSupport() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport strippedSupport = PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(List.of())
                              .build())
            .build();

        // When
        Throwable throwable = crossSideSubmission(existingFlag, strippedSupport);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Disabled("Logic issue in production code")
    @Test
    void shouldRejectManageSupportForAnotherPartysSupport() {
        UUID partyId = UUID.randomUUID();
        PartyEntity otherSideParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>())
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(otherSideParty, USER_ID)).thenReturn(false);

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(partyId.toString(), supportRequest()));
        Set<PartyEntity> existingParties = Set.of(otherSideParty);

        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID, true));

        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(otherSideParty.getDefendantFlags()).isEmpty();
        verifyNoInteractions(flagRefDataRepository);
    }

    @Disabled("Logic issue in production code")
    @Test
    void shouldRejectRequestSupportCreatedAgainstTheOtherSide() {
        // Given
        UUID otherPartyId = UUID.randomUUID();
        PartyEntity otherParty = PartyEntity.builder()
            .id(otherPartyId)
            .defendantFlags(new ArrayList<>())
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(otherParty, USER_ID)).thenReturn(false);

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(otherPartyId.toString(), supportRequest()));
        Set<PartyEntity> existingParties = Set.of(otherParty);

        // When
        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID, true));

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(otherParty.getDefendantFlags()).isEmpty();
    }

    @Disabled
    @Test
    void shouldRejectCrossSideSubmissionWithANullSupportFlagDetail() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport nullDetail = echoedSupport(existingFlag.getId().toString(), null);

        // When
        Throwable throwable = crossSideSubmission(existingFlag, nullDetail);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    private PartySupport supportRequest() {
        return PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(createFlagDetail(null, "RA0042", "Reasonable adjustment",
                                                        "New support request", "Requested"))
                              .build())
            .build();
    }

    private ListValue<PartySupport> createPartySupportListValue(String id, PartySupport partySupport) {
        return ListValue.<PartySupport>builder()
            .id(id)
            .value(partySupport)
            .build();
    }
}
