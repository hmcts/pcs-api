package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartySupportOwnershipResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

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

@ExtendWith(MockitoExtension.class)
class CaseFlagServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDateTime FLAG_CREATED = LocalDateTime.of(2026, 8, 1, 12, 0);
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
        underTest = new CaseFlagService(
            flagRefDataRepository, camundaService, taskDescriptionService,
            partySupportOwnershipResolver, translationWAService
        );
    }

    @Test
    void shouldMergeNewCaseFlags() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(
                null, "CF0002", "Complex Case",
                "Complicated case", "Active"
            ))
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
            .details(createFlagDetail(
                null, "CF0007", "Urgent case",
                "Urgent case test", "Active"
            ))
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
            .details(createFlagDetail(
                null, "CF0002", "Complex Case",
                "Complicated case", "Active"
            ))
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
            .details(createFlagDetail(
                null, "CF0002", "Complex case - renamed",
                "Complicated case", "Active"
            ))
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
                                          ListValue.<String>builder().value("Reasonable adjustment").build()
                                      ))
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
            .details((createFlagDetail(
                existingPartyId.toString(), "PF00015", "Language Interpreter ",
                "Spanish Language Interpreter inactive", "Inactive"
            )))
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
            .details(createFlagDetail(
                null, "PF0026",
                "I want to receive communications and documents in Welsh", "Welsh comms", "Active"
            ))
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
            .details(createFlagDetail(
                null, "PF00015", "Language Interpreter",
                "Spanish Language Interpreter", "Active"
            ))
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
            .details(createFlagDetail(
                null, "PF0026",
                "I want to receive communications and documents in Welsh", "Welsh comms", "Inactive"
            ))
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
            .details(createFlagDetail(
                existingWelshFlag.getId().toString(), "PF0026",
                "I want to receive communications and documents in Welsh", "Welsh comms", "Active"
            ))
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
                                .details(createFlagDetail(
                                    null, "PF0002", "Vulnerable user",
                                    "Internal only flag", "Active"
                                ))
                                .build())
            .partyFlagsExternal(Flags.builder()
                                    .visibility(FlagVisibility.EXTERNAL)
                                    .details(createFlagDetail(
                                        null, "PF0015", "Language Interpreter",
                                        "Externally visible flag", "Requested"
                                    ))
                                    .build())
            .build();

        underTest.mergePartyFlags(List.of(createPartyListValue(partyId.toString(), incomingParty)), partyEntities);

        List<CasePartyFlagEntity> savedFlags = partyEntities.iterator().next().getDefendantFlags();
        assertThat(savedFlags).hasSize(2);
        assertThat(savedFlags)
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactlyInAnyOrder(
                tuple("Internal only flag", "Internal"),
                tuple("Externally visible flag", "External")
            );
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
                                .details(createFlagDetail(
                                    null, "PF0002", "Vulnerable user",
                                    "New internal flag", "Active"
                                ))
                                .build())
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactlyInAnyOrder(
                tuple("New internal flag", "Internal"),
                tuple("Existing external flag", "External")
            );
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
                                    .details(createFlagDetail(
                                        null, "PF0015", "Language Interpreter",
                                        "New external flag", "Requested"
                                    ))
                                    .build())
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactlyInAnyOrder(
                tuple("Existing internal flag", "Internal"),
                tuple("New external flag", "External")
            );
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

    @Test
    void shouldSaveOtherDescriptionsSuppliedForAnExistingPartyFlag() {
        UUID partyId = UUID.randomUUID();
        UUID flagId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createOtherFlagEntity(flagId);
        PartyEntity existingParty = createPartyWithFlags(partyId, existingFlag);

        Party incomingParty = Party.builder()
            .defendantFlags(internalFlags(otherFlagDetail(
                flagId.toString(), "Retired judge on case",
                "Barnwr wedi ymddeol ar yr achos", "Comment"
            )))
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags()).hasSize(1);
        CasePartyFlagEntity savedFlag = existingParty.getDefendantFlags().getFirst();
        assertThat(savedFlag.getOtherDescription()).isEqualTo("Retired judge on case");
        assertThat(savedFlag.getOtherDescriptionWelsh()).isEqualTo("Barnwr wedi ymddeol ar yr achos");
        assertThat(savedFlag.getId()).isEqualTo(flagId);
    }

    @Test
    void shouldRetainOtherDescriptionsOmittedFromAnExistingPartyFlagPayload() {
        UUID partyId = UUID.randomUUID();
        UUID flagId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createOtherFlagEntity(flagId);
        PartyEntity existingParty = createPartyWithFlags(partyId, existingFlag);

        Party incomingParty = Party.builder()
            .defendantFlags(internalFlags(otherFlagDetail(flagId.toString(), null, null, "Updated comment")))
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags()).hasSize(1);
        CasePartyFlagEntity savedFlag = existingParty.getDefendantFlags().getFirst();
        assertThat(savedFlag.getOtherDescription()).isEqualTo("Stored other description");
        assertThat(savedFlag.getOtherDescriptionWelsh()).isEqualTo("Disgrifiad arall wedi ei storio");
        assertThat(savedFlag.getId()).isEqualTo(flagId);
        assertThat(savedFlag.getFlagComment()).isEqualTo("Updated comment");
    }

    @Test
    void shouldRetainOtherDescriptionsWhenThePayloadSuppliesBlankValues() {
        UUID partyId = UUID.randomUUID();
        UUID flagId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createOtherFlagEntity(flagId);
        PartyEntity existingParty = createPartyWithFlags(partyId, existingFlag);

        Party incomingParty = Party.builder()
            .defendantFlags(internalFlags(otherFlagDetail(flagId.toString(), "", "", "Updated comment")))
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        CasePartyFlagEntity savedFlag = existingParty.getDefendantFlags().getFirst();
        assertThat(savedFlag.getOtherDescription()).isEqualTo("Stored other description");
        assertThat(savedFlag.getOtherDescriptionWelsh()).isEqualTo("Disgrifiad arall wedi ei storio");
    }

    @Test
    void shouldRetainSubTypeAndCreationDataOmittedFromAnExistingPartyFlagPayload() {
        UUID partyId = UUID.randomUUID();
        UUID flagId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createOtherFlagEntity(flagId);
        PartyEntity existingParty = createPartyWithFlags(partyId, existingFlag);

        ListValue<FlagDetail> incomingFlagDetail = ListValue.<FlagDetail>builder()
            .id(flagId.toString())
            .value(FlagDetail.builder()
                       .flagCode("OT0001")
                       .name("Other")
                       .status("Inactive")
                       .flagComment("Updated comment")
                       .flagUpdateComment("No longer needed")
                       .dateTimeModified(LocalDateTime.of(2026, 8, 19, 9, 30))
                       .build())
            .build();

        Party incomingParty = Party.builder().defendantFlags(internalFlags(incomingFlagDetail)).build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        CasePartyFlagEntity savedFlag = existingParty.getDefendantFlags().getFirst();
        assertThat(savedFlag.getSubTypeKey()).isEqualTo("OTH");
        assertThat(savedFlag.getSubTypeValue()).isEqualTo("Stored sub type");
        assertThat(savedFlag.getSubTypeValueWelsh()).isEqualTo("Is-fath wedi ei storio");
        assertThat(savedFlag.getPaths()).isEqualTo(":Party");
        assertThat(savedFlag.getDateTimeCreated()).isEqualTo(FLAG_CREATED);
        assertThat(savedFlag.getDefaultStatus()).isEqualTo("Inactive");
        assertThat(savedFlag.getFlagUpdateComment()).isEqualTo("No longer needed");
        assertThat(savedFlag.getDateTimeModified()).isEqualTo(LocalDateTime.of(2026, 8, 19, 9, 30));
    }

    /**
     * A caseworker emptying the comment box sends the field through as an empty value, which clears the
     * stored comment. Contrast with a status-only update, which omits the field entirely - see
     * {@link #shouldRetainAFlagCommentOmittedFromAStatusOnlyUpdate()}.
     */
    @Test
    void shouldClearAFlagCommentTheCaseworkerHasRemoved() {
        UUID partyId = UUID.randomUUID();
        UUID flagId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createOtherFlagEntity(flagId);
        PartyEntity existingParty = createPartyWithFlags(partyId, existingFlag);

        Party incomingParty = Party.builder()
            .defendantFlags(internalFlags(otherFlagDetail(flagId.toString(), null, null, "")))
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags().getFirst().getFlagComment()).isNull();
    }

    @Test
    void shouldRetainAFlagCommentOmittedFromAStatusOnlyUpdate() {
        UUID partyId = UUID.randomUUID();
        UUID flagId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createOtherFlagEntity(flagId);
        PartyEntity existingParty = createPartyWithFlags(partyId, existingFlag);

        ListValue<FlagDetail> statusOnlyUpdate = ListValue.<FlagDetail>builder()
            .id(flagId.toString())
            .value(FlagDetail.builder()
                       .flagCode("OT0001")
                       .name("Other")
                       .status("Inactive")
                       .flagUpdateComment("No longer required")
                       .availableExternally(YesOrNo.NO)
                       .hearingRelevant(YesOrNo.YES)
                       .path(List.of(ListValue.<String>builder().value("Party").build()))
                       .build())
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(
                partyId.toString(),
                Party.builder().defendantFlags(internalFlags(statusOnlyUpdate)).build()
            )),
            Set.of(existingParty)
        );

        CasePartyFlagEntity saved = existingParty.getDefendantFlags().getFirst();
        assertThat(saved.getDefaultStatus()).isEqualTo("Inactive");
        assertThat(saved.getFlagUpdateComment()).isEqualTo("No longer required");
        assertThat(saved.getFlagComment()).isEqualTo("Stored comment");
    }

    @Test
    void shouldClearTheWelshFlagCommentWhenThePayloadCarriesNoValue() {
        UUID partyId = UUID.randomUUID();
        UUID flagId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createOtherFlagEntity(flagId);
        existingFlag.setFlagCommentWelsh("Sylw wedi'i storio");
        PartyEntity existingParty = createPartyWithFlags(partyId, existingFlag);

        underTest.mergePartyFlags(
            List.of(createPartyListValue(
                partyId.toString(),
                Party.builder().defendantFlags(internalFlags(
                    otherFlagDetail(flagId.toString(), null, null, "Still commented"))).build()
            )),
            Set.of(existingParty)
        );

        assertThat(existingParty.getDefendantFlags().getFirst().getFlagCommentWelsh()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Active", "Inactive", "Not approved", "Requested"})
    void shouldRetainAFlagCommentForEveryStatusOnlyUpdate(String newStatus) {
        UUID partyId = UUID.randomUUID();
        UUID flagId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createOtherFlagEntity(flagId);
        PartyEntity existingParty = createPartyWithFlags(partyId, existingFlag);

        ListValue<FlagDetail> statusOnlyUpdate = ListValue.<FlagDetail>builder()
            .id(flagId.toString())
            .value(FlagDetail.builder()
                       .flagCode("OT0001")
                       .name("Other")
                       .status(newStatus)
                       .flagUpdateComment("Reviewed")
                       .availableExternally(YesOrNo.NO)
                       .hearingRelevant(YesOrNo.YES)
                       .path(List.of(ListValue.<String>builder().value("Party").build()))
                       .build())
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(
                partyId.toString(),
                Party.builder().defendantFlags(internalFlags(statusOnlyUpdate)).build()
            )),
            Set.of(existingParty)
        );

        CasePartyFlagEntity saved = existingParty.getDefendantFlags().getFirst();
        assertThat(saved.getDefaultStatus()).isEqualTo(newStatus);
        assertThat(saved.getFlagComment()).isEqualTo("Stored comment");
    }

    @Test
    void shouldCreateANewOtherPartyFlagWithNoStoredFlagId() {
        UUID partyId = UUID.randomUUID();
        Set<PartyEntity> partyEntities = createPartyEntities(partyId);

        Party incomingParty = Party.builder()
            .defendantFlags(internalFlags(otherFlagDetail(
                null, "Brand new other flag",
                "Baner arall newydd sbon", "New comment"
            )))
            .build();

        underTest.mergePartyFlags(List.of(createPartyListValue(partyId.toString(), incomingParty)), partyEntities);

        List<CasePartyFlagEntity> savedFlags = partyEntities.iterator().next().getDefendantFlags();
        assertThat(savedFlags).hasSize(1);
        assertThat(savedFlags.getFirst().getOtherDescription()).isEqualTo("Brand new other flag");
        assertThat(savedFlags.getFirst().getOtherDescriptionWelsh()).isEqualTo("Baner arall newydd sbon");
        assertThat(savedFlags.getFirst().getId()).isNull();
        assertThat(savedFlags.getFirst().getVisibility()).isEqualTo("Internal");
    }

    @Test
    void shouldSaveOtherDescriptionsSuppliedForAnExistingCaseFlag() {
        UUID flagId = UUID.randomUUID();
        CaseFlagEntity existingFlag = createOtherCaseFlagEntity(flagId);
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseFlags(new ArrayList<>(List.of(existingFlag)))
            .build();

        Flags incomingFlags = internalFlags(otherFlagDetail(
            flagId.toString(), "Case other description",
            "Disgrifiad arall achos", "Comment"
        ));

        List<CaseFlagEntity> savedFlags = underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertThat(savedFlags).hasSize(1);
        assertThat(savedFlags.getFirst().getOtherDescription()).isEqualTo("Case other description");
        assertThat(savedFlags.getFirst().getOtherDescriptionWelsh()).isEqualTo("Disgrifiad arall achos");
        assertThat(savedFlags.getFirst().getId()).isEqualTo(flagId);
    }

    @Test
    void shouldRetainOtherDescriptionsOmittedFromAnExistingCaseFlagPayload() {
        UUID flagId = UUID.randomUUID();
        CaseFlagEntity existingFlag = createOtherCaseFlagEntity(flagId);
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseFlags(new ArrayList<>(List.of(existingFlag)))
            .build();

        Flags incomingFlags = internalFlags(otherFlagDetail(flagId.toString(), null, null, "Updated comment"));

        List<CaseFlagEntity> savedFlags = underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertThat(savedFlags).hasSize(1);
        assertThat(savedFlags.getFirst()).isSameAs(existingFlag);
        assertThat(savedFlags.getFirst().getOtherDescription()).isEqualTo("Stored other description");
        assertThat(savedFlags.getFirst().getOtherDescriptionWelsh()).isEqualTo("Disgrifiad arall wedi ei storio");
        assertThat(savedFlags.getFirst().getId()).isEqualTo(flagId);
        assertThat(savedFlags.getFirst().getDateTimeCreated()).isEqualTo(FLAG_CREATED);
        assertThat(savedFlags.getFirst().getFlagComment()).isEqualTo("Updated comment");
    }

    @Test
    void shouldNotMergeAnIncomingInternalFlagIntoAnExistingExternalFlag() {
        UUID partyId = UUID.randomUUID();
        UUID flagId = UUID.randomUUID();
        CasePartyFlagEntity existingExternalFlag = createOtherFlagEntity(flagId);
        existingExternalFlag.setVisibility("External");
        PartyEntity existingParty = createPartyWithFlags(partyId, existingExternalFlag);

        Party incomingParty = Party.builder()
            .defendantFlags(internalFlags(otherFlagDetail(
                flagId.toString(), "Internal other description",
                null, "Internal comment"
            )))
            .build();

        underTest.mergePartyFlags(
            List.of(createPartyListValue(partyId.toString(), incomingParty)), Set.of(existingParty));

        assertThat(existingParty.getDefendantFlags()).hasSize(2);
        assertThat(existingParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getVisibility, BaseCaseFlag::getOtherDescription)
            .containsExactlyInAnyOrder(
                tuple("External", "Stored other description"),
                tuple("Internal", "Internal other description")
            );
        assertThat(existingExternalFlag.getFlagComment()).isEqualTo("Stored comment");
    }

    private Flags internalFlags(ListValue<FlagDetail> flagDetail) {
        return Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(List.of(flagDetail))
            .build();
    }

    private ListValue<FlagDetail> otherFlagDetail(String listValueId, String otherDescription,
                                                  String otherDescriptionCy, String flagComment) {
        return ListValue.<FlagDetail>builder()
            .id(listValueId)
            .value(FlagDetail.builder()
                       .flagCode("OT0001")
                       .name("Other")
                       .status("Active")
                       .flagComment(flagComment)
                       .otherDescription(otherDescription)
                       .otherDescriptionCy(otherDescriptionCy)
                       .availableExternally(YesOrNo.NO)
                       .hearingRelevant(YesOrNo.YES)
                       .path(List.of(ListValue.<String>builder().value("Party").build()))
                       .build())
            .build();
    }

    private CasePartyFlagEntity createOtherFlagEntity(UUID flagId) {
        CasePartyFlagEntity flagEntity = new CasePartyFlagEntity();
        flagEntity.setId(flagId);
        flagEntity.setFlagRefData(FlagRefDataEntity.builder().flagCode("OT0001").build());
        flagEntity.setDefaultStatus("Active");
        flagEntity.setVisibility("Internal");
        flagEntity.setFlagComment("Stored comment");
        applyStoredFlagData(flagEntity);

        return flagEntity;
    }

    private CaseFlagEntity createOtherCaseFlagEntity(UUID flagId) {
        CaseFlagEntity flagEntity = new CaseFlagEntity();
        flagEntity.setId(flagId);
        flagEntity.setFlagRefData(FlagRefDataEntity.builder().flagCode("OT0001").build());
        flagEntity.setDefaultStatus("Active");
        flagEntity.setVisibility("Internal");
        flagEntity.setFlagComment("Stored comment");
        applyStoredFlagData(flagEntity);

        return flagEntity;
    }

    private void applyStoredFlagData(BaseCaseFlag flagEntity) {
        flagEntity.setOtherDescription("Stored other description");
        flagEntity.setOtherDescriptionWelsh("Disgrifiad arall wedi ei storio");
        flagEntity.setSubTypeKey("OTH");
        flagEntity.setSubTypeValue("Stored sub type");
        flagEntity.setSubTypeValueWelsh("Is-fath wedi ei storio");
        flagEntity.setPaths(":Party");
        flagEntity.setDateTimeCreated(FLAG_CREATED);
    }

    private PartyEntity createPartyWithFlags(UUID partyId, CasePartyFlagEntity... flags) {
        return PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(flags)))
            .build();
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
                              .details(createFlagDetail(
                                  null, "RA0042", "Reasonable adjustment",
                                  "New support request", "Requested"
                              ))
                              .build())
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(existingParty, USER_ID)).thenReturn(true);

        underTest.mergePartySupportFlags(
            List.of(createPartySupportListValue(partyId.toString(), incomingSupport)),
            Set.of(existingParty), USER_ID
        );

        assertThat(existingParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactlyInAnyOrder(
                tuple("Existing internal flag", "Internal"),
                tuple("New support request", "External")
            );
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
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID));

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
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID));

        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("Support submitted for an invalid party reference");
        verifyNoInteractions(flagRefDataRepository);
    }

    @Test
    void shouldAllowSupportForARepresentedPartyWhenOtherPartiesAreUnchanged() {
        // Given
        UUID representedPartyId = UUID.randomUUID();
        UUID unrepresentedPartyId = UUID.randomUUID();

        CasePartyFlagEntity unrepresentedPartyFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Unrepresented party existing support");
        unrepresentedPartyFlag.setVisibility("External");

        CasePartyFlagEntity unrepresentedPartyInternalFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Unrepresented party internal flag");
        unrepresentedPartyInternalFlag.setVisibility("Internal");

        PartyEntity representedParty = PartyEntity.builder()
            .id(representedPartyId)
            .defendantFlags(new ArrayList<>())
            .build();
        PartyEntity unrepresentedParty = PartyEntity.builder()
            .id(unrepresentedPartyId)
            .defendantFlags(new ArrayList<>(List.of(unrepresentedPartyFlag, unrepresentedPartyInternalFlag)))
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(representedParty, USER_ID)).thenReturn(true);
        when(partySupportOwnershipResolver.isOwnedByUser(unrepresentedParty, USER_ID)).thenReturn(false);

        // When
        underTest.mergePartySupportFlags(
            List.of(
                createPartySupportListValue(representedPartyId.toString(), supportRequest()),
                createPartySupportListValue(
                    unrepresentedPartyId.toString(),
                    unchangedSupport(unrepresentedPartyFlag)
                )
            ),
            new HashSet<>(List.of(representedParty, unrepresentedParty)), USER_ID
        );

        // Then
        assertThat(representedParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment)
            .containsExactly("New support request");
        assertThat(unrepresentedParty.getDefendantFlags())
            .containsExactlyInAnyOrder(unrepresentedPartyFlag, unrepresentedPartyInternalFlag);
    }

    @Test
    void shouldRejectChangeToAnUnrepresentedPartysExistingSupport() {
        // Given
        UUID unrepresentedPartyId = UUID.randomUUID();
        CasePartyFlagEntity unrepresentedPartyFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Unrepresented party existing support");
        unrepresentedPartyFlag.setVisibility("External");

        PartyEntity unrepresentedParty = PartyEntity.builder()
            .id(unrepresentedPartyId)
            .defendantFlags(new ArrayList<>(List.of(unrepresentedPartyFlag)))
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(unrepresentedParty, USER_ID)).thenReturn(false);

        PartySupport mutatedSupport = PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(List.of(ListValue.<FlagDetail>builder()
                                                   .id(unrepresentedPartyFlag.getId().toString())
                                                   .value(FlagDetail.builder()
                                                              .status("Inactive")
                                                              .flagComment(unrepresentedPartyFlag.getFlagComment())
                                                              .flagUpdateComment("Withdrawn without authority")
                                                              .build())
                                                   .build()))
                              .build())
            .build();

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(unrepresentedPartyId.toString(), mutatedSupport));
        Set<PartyEntity> existingParties = Set.of(unrepresentedParty);

        // When
        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID));

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(unrepresentedParty.getDefendantFlags()).containsExactly(unrepresentedPartyFlag);
        assertThat(unrepresentedPartyFlag.getDefaultStatus()).isEqualTo("Active");
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
            Set.of(existingParty), USER_ID
        );

        // Then
        assertThat(existingParty.getDefendantFlags()).containsExactly(existingFlag);
        verifyNoInteractions(flagRefDataRepository);
    }

    @Test
    void shouldRejectManageSupportForAnUnrepresentedPartyWithExistingSupport() {
        UUID partyId = UUID.randomUUID();
        PartyEntity unrepresentedParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(existingExternalFlag())))
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(unrepresentedParty, USER_ID)).thenReturn(false);

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(partyId.toString(), supportRequest()));
        Set<PartyEntity> existingParties = Set.of(unrepresentedParty);

        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID));

        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(unrepresentedParty.getDefendantFlags()).isNotEmpty();
        verifyNoInteractions(flagRefDataRepository);
    }

    @Test
    void shouldAllowManageSupportForARepresentedParty() {
        UUID partyId = UUID.randomUUID();
        PartyEntity representedParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>())
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(representedParty, USER_ID)).thenReturn(true);

        underTest.mergePartySupportFlags(
            List.of(createPartySupportListValue(partyId.toString(), supportRequest())),
            Set.of(representedParty), USER_ID
        );

        assertThat(representedParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
            .containsExactly(tuple("New support request", "External"));
    }

    @Test
    void shouldAllowInactivatingExistingSupportForARepresentedParty() {
        // Given
        UUID partyId = UUID.randomUUID();
        CasePartyFlagEntity existingFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Support previously requested");
        existingFlag.setVisibility("External");

        PartyEntity representedParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>(List.of(existingFlag)))
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(representedParty, USER_ID)).thenReturn(true);

        PartySupport inactivated = PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(createFlagDetail(
                                  existingFlag.getId().toString(), "RA0042",
                                  "Reasonable adjustment",
                                  "Support previously requested", "Inactive"
                              ))
                              .build())
            .build();

        // When
        underTest.mergePartySupportFlags(
            List.of(createPartySupportListValue(partyId.toString(), inactivated)),
            Set.of(representedParty), USER_ID
        );

        // Then
        assertThat(representedParty.getDefendantFlags())
            .extracting(BaseCaseFlag::getDefaultStatus, BaseCaseFlag::getVisibility)
            .containsExactly(tuple("Inactive", "External"));
    }

    @Test
    void shouldRejectSupportCreatedForAnUnrepresentedPartyWithNoExistingSupport() {
        UUID partyId = UUID.randomUUID();
        PartyEntity unrepresentedParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>())
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(unrepresentedParty, USER_ID)).thenReturn(false);

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(partyId.toString(), supportRequest()));
        Set<PartyEntity> existingParties = Set.of(unrepresentedParty);

        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID));

        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(unrepresentedParty.getDefendantFlags()).isEmpty();
        verifyNoInteractions(flagRefDataRepository);
    }

    private PartySupport unchangedSupport(CasePartyFlagEntity existingFlag) {
        return echoedSupport(
            existingFlag.getId().toString(),
            FlagDetail.builder()
                .status(existingFlag.getDefaultStatus())
                .flagComment(existingFlag.getFlagComment())
                .flagUpdateComment(existingFlag.getFlagUpdateComment())
                .build()
        );
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

    private Throwable submissionForUnrepresentedParty(CasePartyFlagEntity existingFlag, PartySupport incomingSupport) {
        PartyEntity unrepresentedParty = PartyEntity.builder()
            .id(UUID.randomUUID())
            .defendantFlags(new ArrayList<>(List.of(existingFlag)))
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(unrepresentedParty, USER_ID)).thenReturn(false);

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(unrepresentedParty.getId().toString(), incomingSupport));
        Set<PartyEntity> existingParties = Set.of(unrepresentedParty);

        return catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID));
    }

    private CasePartyFlagEntity existingExternalFlag() {
        CasePartyFlagEntity existingFlag = createCasePartyFlagEntity(
            UUID.randomUUID(), "Active", "Unrepresented party existing support");
        existingFlag.setVisibility("External");
        existingFlag.setFlagUpdateComment("Original update comment");
        return existingFlag;
    }

    @Test
    void shouldRejectAnUnrepresentedPartysFlagBeingReusedForADifferentFlagType() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport differentFlagType = PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(List.of(ListValue.<FlagDetail>builder()
                                                   .id(existingFlag.getId().toString())
                                                   .value(FlagDetail.builder()
                                                              .flagCode("RA0042")
                                                              .name("A different flag type")
                                                              .status(existingFlag.getDefaultStatus())
                                                              .flagComment(existingFlag.getFlagComment())
                                                              .flagUpdateComment(existingFlag.getFlagUpdateComment())
                                                              .build())
                                                   .build()))
                              .build())
            .build();

        // When
        Throwable throwable = submissionForUnrepresentedParty(existingFlag, differentFlagType);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Test
    void shouldRejectOtherDescriptionChangeToAnUnrepresentedPartysSupport() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        existingFlag.setOtherDescription("Stored other description");
        PartySupport descriptionChanged = PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(List.of(ListValue.<FlagDetail>builder()
                                                   .id(existingFlag.getId().toString())
                                                   .value(FlagDetail.builder()
                                                              .flagCode("PF00015")
                                                              .status(existingFlag.getDefaultStatus())
                                                              .flagComment(existingFlag.getFlagComment())
                                                              .flagUpdateComment(existingFlag.getFlagUpdateComment())
                                                              .otherDescription("Rewritten without authority")
                                                              .build())
                                                   .build()))
                              .build())
            .build();

        // When
        Throwable throwable = submissionForUnrepresentedParty(existingFlag, descriptionChanged);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(existingFlag.getOtherDescription()).isEqualTo("Stored other description");
    }

    @Test
    void shouldRejectSubTypeOrDescriptionChangesToAnUnrepresentedPartysSupport() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        existingFlag.setSubTypeValue("Stored sub type");
        PartySupport subTypeChanged = PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(List.of(ListValue.<FlagDetail>builder()
                                                   .id(existingFlag.getId().toString())
                                                   .value(FlagDetail.builder()
                                                              .flagCode("PF00015")
                                                              .status(existingFlag.getDefaultStatus())
                                                              .flagComment(existingFlag.getFlagComment())
                                                              .flagUpdateComment(existingFlag.getFlagUpdateComment())
                                                              .subTypeValue("Changed without authority")
                                                              .build())
                                                   .build()))
                              .build())
            .build();

        // When
        Throwable throwable = submissionForUnrepresentedParty(existingFlag, subTypeChanged);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Test
    void shouldStillTolerateAnEchoedFlagThatOmitsCarriedThroughFields() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        existingFlag.setSubTypeValue("Stored sub type");
        existingFlag.setOtherDescription("Stored other description");
        PartySupport echoedWithoutCarriedFields = echoedSupport(
            existingFlag.getId().toString(),
            FlagDetail.builder()
                .status(existingFlag.getDefaultStatus())
                .flagComment(existingFlag.getFlagComment())
                .flagUpdateComment(
                    existingFlag.getFlagUpdateComment())
                .build()
        );

        // When
        Throwable throwable = submissionForUnrepresentedParty(existingFlag, echoedWithoutCarriedFields);

        // Then
        assertThat(throwable).isNull();
    }

    @Test
    void shouldRejectCommentChangeToAnUnrepresentedPartysSupport() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport commentChanged = echoedSupport(
            existingFlag.getId().toString(),
            FlagDetail.builder()
                .status(existingFlag.getDefaultStatus())
                .flagComment("Reworded without authority")
                .flagUpdateComment(existingFlag.getFlagUpdateComment())
                .build()
        );

        // When
        Throwable throwable = submissionForUnrepresentedParty(existingFlag, commentChanged);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Test
    void shouldRejectUpdateCommentChangeToAnUnrepresentedPartysSupport() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport updateCommentChanged = echoedSupport(
            existingFlag.getId().toString(),
            FlagDetail.builder()
                .status(existingFlag.getDefaultStatus())
                .flagComment(existingFlag.getFlagComment())
                .flagUpdateComment("Amended without authority")
                .build()
        );

        // When
        Throwable throwable = submissionForUnrepresentedParty(existingFlag, updateCommentChanged);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Test
    void shouldRejectUnrepresentedPartySubmissionReferencingAnUnknownFlagId() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport unknownFlagId = echoedSupport(
            UUID.randomUUID().toString(),
            FlagDetail.builder()
                .status(existingFlag.getDefaultStatus())
                .flagComment(existingFlag.getFlagComment())
                .flagUpdateComment(existingFlag.getFlagUpdateComment())
                .build()
        );

        // When
        Throwable throwable = submissionForUnrepresentedParty(existingFlag, unknownFlagId);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Test
    void shouldRejectRemovalOfAnUnrepresentedPartysSupport() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport strippedSupport = PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(List.of())
                              .build())
            .build();

        // When
        Throwable throwable = submissionForUnrepresentedParty(existingFlag, strippedSupport);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    @Test
    void shouldRejectManageSupportForAnUnrepresentedParty() {
        UUID partyId = UUID.randomUUID();
        PartyEntity unrepresentedParty = PartyEntity.builder()
            .id(partyId)
            .defendantFlags(new ArrayList<>())
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(unrepresentedParty, USER_ID)).thenReturn(false);

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(partyId.toString(), supportRequest()));
        Set<PartyEntity> existingParties = Set.of(unrepresentedParty);

        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID));

        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(unrepresentedParty.getDefendantFlags()).isEmpty();
        verifyNoInteractions(flagRefDataRepository);
    }

    @Test
    void shouldRejectRequestSupportCreatedForAnUnrepresentedParty() {
        // Given
        UUID unrepresentedPartyId = UUID.randomUUID();
        PartyEntity unrepresentedParty = PartyEntity.builder()
            .id(unrepresentedPartyId)
            .defendantFlags(new ArrayList<>())
            .build();

        when(partySupportOwnershipResolver.isOwnedByUser(unrepresentedParty, USER_ID)).thenReturn(false);

        List<ListValue<PartySupport>> incoming =
            List.of(createPartySupportListValue(unrepresentedPartyId.toString(), supportRequest()));
        Set<PartyEntity> existingParties = Set.of(unrepresentedParty);

        // When
        Throwable throwable = catchThrowable(
            () -> underTest.mergePartySupportFlags(incoming, existingParties, USER_ID));

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
        assertThat(unrepresentedParty.getDefendantFlags()).isEmpty();
    }

    @Test
    void shouldRejectUnrepresentedPartySubmissionWithANullFlagDetail() {
        // Given
        CasePartyFlagEntity existingFlag = existingExternalFlag();
        PartySupport nullDetail = echoedSupport(existingFlag.getId().toString(), null);

        // When
        Throwable throwable = submissionForUnrepresentedParty(existingFlag, nullDetail);

        // Then
        assertThat(throwable)
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User cannot change support for this party on this case");
    }

    private PartySupport supportRequest() {
        return PartySupport.builder()
            .supportFlags(Flags.builder()
                              .visibility(FlagVisibility.EXTERNAL)
                              .details(createFlagDetail(
                                  null, "RA0042", "Reasonable adjustment",
                                  "New support request", "Requested"
                              ))
                              .build())
            .build();
    }

    private ListValue<PartySupport> createPartySupportListValue(String id, PartySupport partySupport) {
        return ListValue.<PartySupport>builder()
            .id(id)
            .value(partySupport)
            .build();
    }

    @Nested
    class ExternalPersonaScenarios {

        private static final String CLAIMANT_FIRM = "CLAIMANT-FIRM";
        private static final String DEFENDANT_FIRM = "DEFENDANT-FIRM";
        private static final UUID SOLICITOR_USER_ID = UUID.randomUUID();

        @Mock
        private OrganisationService organisationService;

        private CaseFlagService caseFlagService;

        @BeforeEach
        void setUp() {
            caseFlagService = new CaseFlagService(
                flagRefDataRepository, camundaService, taskDescriptionService,
                new PartySupportOwnershipResolver(organisationService),
                translationWAService
            );
        }

        @Test
        void shouldAllowAPartyUserToCreateSupportForThemselves() {
            PartyEntity self = partyForUser(USER_ID);

            merge(self, supportRequest(), USER_ID);

            assertThat(self.getDefendantFlags())
                .extracting(BaseCaseFlag::getFlagComment, BaseCaseFlag::getVisibility)
                .containsExactly(tuple("New support request", "External"));
        }

        @Test
        void shouldAllowAPartyUserToInactivateTheirOwnSupport() {
            CasePartyFlagEntity existing = externalFlag();
            PartyEntity self = partyForUser(USER_ID);
            self.getDefendantFlags().add(existing);

            merge(self, inactivating(existing), USER_ID);

            assertThat(self.getDefendantFlags())
                .extracting(BaseCaseFlag::getDefaultStatus)
                .containsExactly("Inactive");
        }

        @Test
        void shouldAllowClaimantSolicitorToCreateSupportForTheirRepresentedClaimant() {
            PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);

            merge(claimant, supportRequest(), SOLICITOR_USER_ID);

            assertThat(claimant.getDefendantFlags())
                .extracting(BaseCaseFlag::getFlagComment)
                .containsExactly("New support request");
        }

        @Test
        void shouldAllowClaimantSolicitorToInactivateTheirRepresentedClaimantsSupport() {
            CasePartyFlagEntity existing = externalFlag();
            PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);
            claimant.getDefendantFlags().add(existing);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);

            merge(claimant, inactivating(existing), SOLICITOR_USER_ID);

            assertThat(claimant.getDefendantFlags())
                .extracting(BaseCaseFlag::getDefaultStatus)
                .containsExactly("Inactive");
        }

        @Test
        void shouldRejectClaimantSolicitorCreatingSupportForADefendant() {
            PartyEntity defendant = defendantRepresentedBy(DEFENDANT_FIRM);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);

            Throwable throwable = catchThrowable(() -> merge(defendant, supportRequest(), SOLICITOR_USER_ID));

            assertRejected(throwable);
            assertThat(defendant.getDefendantFlags()).isEmpty();
        }

        @Test
        void shouldRejectClaimantSolicitorManagingADefendantsSupport() {
            CasePartyFlagEntity existing = externalFlag();
            PartyEntity defendant = defendantRepresentedBy(DEFENDANT_FIRM);
            defendant.getDefendantFlags().add(existing);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);

            Throwable throwable = catchThrowable(() -> merge(defendant, inactivating(existing), SOLICITOR_USER_ID));

            assertRejected(throwable);
            assertThat(defendant.getDefendantFlags()).containsExactly(existing);
            assertThat(existing.getDefaultStatus()).isEqualTo("Active");
        }

        @Test
        void shouldAllowDefendantSolicitorToCreateSupportForTheirRepresentedDefendant() {
            PartyEntity defendant = defendantRepresentedBy(DEFENDANT_FIRM);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(DEFENDANT_FIRM);

            merge(defendant, supportRequest(), SOLICITOR_USER_ID);

            assertThat(defendant.getDefendantFlags())
                .extracting(BaseCaseFlag::getFlagComment)
                .containsExactly("New support request");
        }

        @Test
        void shouldAllowDefendantSolicitorToInactivateTheirRepresentedDefendantsSupport() {
            CasePartyFlagEntity existing = externalFlag();
            PartyEntity defendant = defendantRepresentedBy(DEFENDANT_FIRM);
            defendant.getDefendantFlags().add(existing);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(DEFENDANT_FIRM);

            merge(defendant, inactivating(existing), SOLICITOR_USER_ID);

            assertThat(defendant.getDefendantFlags())
                .extracting(BaseCaseFlag::getDefaultStatus)
                .containsExactly("Inactive");
        }

        @Test
        void shouldRejectDefendantSolicitorCreatingSupportForTheClaimant() {
            PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(DEFENDANT_FIRM);

            Throwable throwable = catchThrowable(() -> merge(claimant, supportRequest(), SOLICITOR_USER_ID));

            assertRejected(throwable);
            assertThat(claimant.getDefendantFlags()).isEmpty();
        }

        @Test
        void shouldRejectDefendantSolicitorManagingTheClaimantsSupport() {
            CasePartyFlagEntity existing = externalFlag();
            PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);
            claimant.getDefendantFlags().add(existing);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(DEFENDANT_FIRM);

            Throwable throwable = catchThrowable(() -> merge(claimant, inactivating(existing), SOLICITOR_USER_ID));

            assertRejected(throwable);
            assertThat(claimant.getDefendantFlags()).containsExactly(existing);
        }

        @Test
        void shouldUpdateTheStoredFlagInPlaceWhenARepresentedPartysSupportIsChanged() {
            CasePartyFlagEntity existing = externalFlag();
            existing.setOtherDescription("Stored other description");
            PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);
            claimant.getDefendantFlags().add(existing);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);

            merge(claimant, inactivating(existing), SOLICITOR_USER_ID);

            assertThat(claimant.getDefendantFlags()).hasSize(1);
            CasePartyFlagEntity savedFlag = claimant.getDefendantFlags().getFirst();
            assertThat(savedFlag).isSameAs(existing);
            assertThat(savedFlag.getId()).isEqualTo(existing.getId());
            assertThat(savedFlag.getOtherDescription()).isEqualTo("Stored other description");
            assertThat(savedFlag.getDefaultStatus()).isEqualTo("Inactive");
        }

        /**
         * The sequence QA reported: a claimant solicitor is refused for a defendant, the defendant's own
         * solicitor then raises support, and the claimant solicitor must still be refused afterwards —
         * whether they add another flag or reuse the stored flag's id for a different flag type.
         */
        @Test
        void shouldKeepRefusingAClaimantSolicitorAfterTheDefendantSolicitorHasRaisedSupport() {
            PartyEntity defendant = defendantRepresentedBy(DEFENDANT_FIRM);

            // The defendant has no support yet: the claimant solicitor is refused
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);
            assertRejected(catchThrowable(() -> merge(defendant, supportRequest(), SOLICITOR_USER_ID)));
            assertThat(defendant.getDefendantFlags()).isEmpty();

            // The defendant's own solicitor raises support, which is allowed
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(DEFENDANT_FIRM);
            merge(defendant, supportRequest(), SOLICITOR_USER_ID);
            assertThat(defendant.getDefendantFlags()).hasSize(1);

            CasePartyFlagEntity raisedFlag = defendant.getDefendantFlags().getFirst();
            raisedFlag.setId(UUID.randomUUID());               // stands in for the id assigned on flush
            final String raisedFlagCode = raisedFlag.getFlagRefData().getFlagCode();

            // Back as the claimant solicitor: adding a second flag is still refused
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);
            assertRejected(catchThrowable(() -> merge(defendant, supportRequest(), SOLICITOR_USER_ID)));

            // ...and so is reusing the stored flag's id for a different flag type
            PartySupport reusedId = PartySupport.builder()
                .supportFlags(Flags.builder()
                                  .visibility(FlagVisibility.EXTERNAL)
                                  .details(List.of(ListValue.<FlagDetail>builder()
                                                       .id(raisedFlag.getId().toString())
                                                       .value(FlagDetail.builder()
                                                                  .flagCode("PF0026")
                                                                  .name("A different flag type")
                                                                  .status(raisedFlag.getDefaultStatus())
                                                                  .flagComment(raisedFlag.getFlagComment())
                                                                  .flagUpdateComment(raisedFlag.getFlagUpdateComment())
                                                                  .build())
                                                       .build()))
                                  .build())
                .build();
            assertRejected(catchThrowable(() -> merge(defendant, reusedId, SOLICITOR_USER_ID)));

            // Neither refusal touched the defendant's stored support
            assertThat(defendant.getDefendantFlags()).containsExactly(raisedFlag);
            assertThat(raisedFlag.getFlagRefData().getFlagCode()).isEqualTo(raisedFlagCode);
        }

        @Test
        void shouldRejectSolicitorActingForAPartyWhoseRepresentationHasEnded() {
            PartyEntity defendant = defendantRepresentedBy(DEFENDANT_FIRM);
            defendant.getClaimPartyOrganisationList().getFirst().setActive(YesOrNo.NO);

            Throwable throwable = catchThrowable(() -> merge(defendant, supportRequest(), SOLICITOR_USER_ID));

            assertRejected(throwable);
            assertThat(defendant.getDefendantFlags()).isEmpty();
        }

        @Test
        void shouldNotTreatAMissingOrganisationAsAMatch() {
            PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(null);

            Throwable throwable = catchThrowable(() -> merge(claimant, supportRequest(), SOLICITOR_USER_ID));

            assertRejected(throwable);
            assertThat(claimant.getDefendantFlags()).isEmpty();
        }

        @Test
        void shouldRetainAnUnrepresentedPartysSupportWhenTheirEntryArrivesUnchanged() {
            CasePartyFlagEntity untouched = externalFlag();
            PartyEntity claimant = claimantRepresentedBy(CLAIMANT_FIRM);
            PartyEntity defendant = defendantRepresentedBy(DEFENDANT_FIRM);
            defendant.getDefendantFlags().add(untouched);
            when(organisationService.getOrganisationIdForCurrentUser()).thenReturn(CLAIMANT_FIRM);

            caseFlagService.mergePartySupportFlags(
                List.of(
                    createPartySupportListValue(claimant.getId().toString(), supportRequest()),
                    createPartySupportListValue(defendant.getId().toString(), echoed(untouched))
                ),
                new HashSet<>(List.of(claimant, defendant)), SOLICITOR_USER_ID
            );

            assertThat(claimant.getDefendantFlags())
                .extracting(BaseCaseFlag::getFlagComment)
                .containsExactly("New support request");
            assertThat(defendant.getDefendantFlags()).containsExactly(untouched);
        }

        private void merge(PartyEntity party, PartySupport incoming, UUID authenticatedUserId) {
            caseFlagService.mergePartySupportFlags(
                List.of(createPartySupportListValue(party.getId().toString(), incoming)),
                Set.of(party), authenticatedUserId
            );
        }

        private void assertRejected(Throwable throwable) {
            assertThat(throwable)
                .isInstanceOf(CaseAccessException.class)
                .hasMessage("User cannot change support for this party on this case");
        }

        private PartySupport inactivating(CasePartyFlagEntity existing) {
            return PartySupport.builder()
                .supportFlags(Flags.builder()
                                  .visibility(FlagVisibility.EXTERNAL)
                                  .details(createFlagDetail(
                                      existing.getId().toString(), "RA0042",
                                      "Reasonable adjustment",
                                      existing.getFlagComment(), "Inactive"
                                  ))
                                  .build())
                .build();
        }

        private PartySupport echoed(CasePartyFlagEntity existing) {
            return PartySupport.builder()
                .supportFlags(Flags.builder()
                                  .visibility(FlagVisibility.EXTERNAL)
                                  .details(List.of(ListValue.<FlagDetail>builder()
                                                       .id(existing.getId().toString())
                                                       .value(FlagDetail.builder()
                                                                  .status(existing.getDefaultStatus())
                                                                  .flagComment(existing.getFlagComment())
                                                                  .flagUpdateComment(existing.getFlagUpdateComment())
                                                                  .build())
                                                       .build()))
                                  .build())
                .build();
        }

        private CasePartyFlagEntity externalFlag() {
            CasePartyFlagEntity flag = createCasePartyFlagEntity(
                UUID.randomUUID(), "Active", "Existing support");
            flag.setVisibility("External");
            return flag;
        }

        private PartyEntity partyForUser(UUID idamId) {
            return PartyEntity.builder()
                .id(UUID.randomUUID())
                .idamId(idamId)
                .claimPartyOrganisationList(new ArrayList<>())
                .defendantFlags(new ArrayList<>())
                .build();
        }

        private PartyEntity claimantRepresentedBy(String organisationId) {
            return PartyEntity.builder()
                .id(UUID.randomUUID())
                .idamId(UUID.randomUUID())
                .organisationId(organisationId)
                .claimPartyOrganisationList(new ArrayList<>())
                .defendantFlags(new ArrayList<>())
                .build();
        }

        private PartyEntity defendantRepresentedBy(String firmOrganisationId) {
            OrganisationEntity organisation = OrganisationEntity.builder()
                .organisationId(firmOrganisationId)
                .organisationName("Representing firm")
                .build();

            ClaimPartyOrganisationEntity link = ClaimPartyOrganisationEntity.builder()
                .organisation(organisation)
                .active(YesOrNo.YES)
                .build();

            return PartyEntity.builder()
                .id(UUID.randomUUID())
                .idamId(UUID.randomUUID())
                .claimPartyOrganisationList(new ArrayList<>(List.of(link)))
                .defendantFlags(new ArrayList<>())
                .build();
        }
    }
}
