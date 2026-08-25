package uk.gov.hmcts.reform.pcs.ccd.service;

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
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TaskDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.service.workallocation.TranslationWAService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

//import static org.assertj.core.api.Assertions.assertE;

@ExtendWith(MockitoExtension.class)
class CaseFlagServiceTest {

    @Mock
    private FlagRefDataRepository flagRefDataRepository;

    @Mock
    private CamundaService camundaService;

    @Mock
    private TaskDescriptionService taskDescriptionService;

    @Mock
    private TranslationWAService translationWAService;

    @InjectMocks
    private CaseFlagService underTest;

    private static final long CASE_REFERENCE = 1234L;

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
    void shouldCreateReviewCaseFlagRequestTaskWhenCaseFlagRequested() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null, "CF0002", "Complex Case",
                                      "Complicated case", "Requested"))
            .build();

        when(taskDescriptionService.createReviewCaseFlagRequestDescription(
            CASE_REFERENCE, List.of("Complex Case"))
        ).thenReturn("request description");

        // When
        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        // Then
        verify(taskDescriptionService).createReviewCaseFlagRequestDescription(
            CASE_REFERENCE, List.of("Complex Case")
        );
        verify(camundaService).createTask(
            CASE_REFERENCE,
            TaskType.REVIEW_CASE_FLAG_REQUEST,
            "request description"
        );
    }

    @Test
    void shouldAmendExistingCaseFlags() {
        // Given
        UUID id = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity(id);
        List<ListValue<FlagDetail>> flagDetails = new ArrayList<>();
        flagDetails.addAll(createFlagDetail(id.toString(),"CF0008", "Power of arrest with Police ",
                                            "Police arrest inactive", "Inactive"));
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
    void shouldCreateOneReviewCaseFlagRequestTaskForMultipleRequestedReasonableAdjustments() {
        // Given
        List<ListValue<FlagDetail>> details = new ArrayList<>();
        details.addAll(createFlagDetailsWithoutIds("RA0033", "Sign language interpreter", "Requested"));
        details.addAll(createFlagDetailsWithoutIds("RA0012", "Braille documents", "Requested"));

        List<String> requestedFlags = List.of("Sign language interpreter", "Braille documents");
        when(taskDescriptionService.createReviewCaseFlagRequestDescription(CASE_REFERENCE, requestedFlags))
            .thenReturn("request description");
        PartyEntity partyEntity = PartyEntity.builder()
            .id(UUID.randomUUID())
            .defendantFlags(new ArrayList<>())
            .build();

        // When
        underTest.saveReasonableAdjustmentFlags(partyEntity, Flags.builder().details(details).build(), CASE_REFERENCE);

        // Then
        assertThat(partyEntity.getDefendantFlags())
            .extracting(flag -> flag.getFlagRefData().getFlagCode())
            .containsExactlyInAnyOrder("RA0033", "RA0012");

        verify(taskDescriptionService).createReviewCaseFlagRequestDescription(CASE_REFERENCE, requestedFlags);
        verify(camundaService).createTask(
            CASE_REFERENCE,
            TaskType.REVIEW_CASE_FLAG_REQUEST,
            "request description"
        );
        verifyNoMoreInteractions(camundaService);
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
                                            .status("Inactive")
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
        return createFlagDetailsWithoutIds(flagCode, name, "Active");
    }

    private List<ListValue<FlagDetail>> createFlagDetailsWithoutIds(String flagCode, String name, String status) {
        return List.of(ListValue.<FlagDetail>builder()
                           .value(FlagDetail.builder()
                                      .flagCode(flagCode)
                                      .name(name)
                                      .status(status)
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
}
