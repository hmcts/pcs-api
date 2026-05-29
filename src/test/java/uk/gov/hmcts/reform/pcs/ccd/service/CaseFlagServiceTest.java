package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CaseFlagServiceTest {

    @Mock
    private FlagRefDataRepository flagRefDataRepository;

    @InjectMocks
    private CaseFlagService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseFlagService(flagRefDataRepository);
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
