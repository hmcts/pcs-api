package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseFlagServiceTest {

    private CaseFlagService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseFlagService();
    }

    @Test
    void shouldMergeNewCaseFlags() {
        // Given
        //UUID id = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build(); //createPcsCaseEntity(id, "Active");

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null,"CF0002", "Complex Case",
                                              "Complicated case", "Active"))
            .build();

        // When
        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        // Then
        assertNotNull(pcsCaseEntity.getCaseFlags());
        List<FlagDetailsEntity> savedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("CF0002", savedFlags.getFirst().getFlagCode());
        assertEquals("Complicated case", savedFlags.getFirst().getFlagComment());
        assertEquals("Complex Case", savedFlags.getFirst().getName());
        assertEquals(1, savedFlags.size());
    }

    @Test
    void shouldAmendExistingCaseFlags() {
        // Given
        UUID id = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity(id);
        List<ListValue<FlagDetail>> flagDetails = new ArrayList<>();
        flagDetails.addAll(createFlagDetail(null, "CF0002", "Complex Case",
                                            "Complicated case", "Active"
        ));
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
        List<FlagDetailsEntity> savedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("CF0002", savedFlags.getFirst().getFlagCode());
        assertEquals("Complicated case", savedFlags.getFirst().getFlagComment());
        assertEquals("Complex Case", savedFlags.getFirst().getName());
        assertEquals(2, savedFlags.size());
        assertThat(savedFlags).extracting(FlagDetailsEntity::getFlagCode).containsExactly("CF0002", "CF0008");
        assertThat(savedFlags.getLast().getFlagComment()).isEqualTo("Police arrest inactive");
        assertThat(savedFlags.getLast().getFlagCode()).isEqualTo("CF0008");
    }

    @Test
    void testMergePartyFlags_NewPartyWithFlags() {
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().parties(new HashSet<>()).build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(List.of(createFlagDetail("FLAG_CODE_1", "Test Flag Comment 1")))
            .build();

        Party incomingParty = Party.builder().appellantFlags(incomingFlags).build();
        List<ListValue<Party>> parties = List.of(createPartyListValue(UUID.randomUUID().toString(), incomingParty));

        caseFlagService.mergePartyFlags(parties, pcsCaseEntity);

        assertEquals(1, pcsCaseEntity.getParties().size());
        PartyEntity savedParty = pcsCaseEntity.getParties().iterator().next();

        assertNotNull(savedParty.getAppellantFlags());
        assertEquals(1, savedParty.getAppellantFlags().size());

        FlagsEntity savedFlags = savedParty.getAppellantFlags().get(0);
        assertEquals("Internal", savedFlags.getVisibility());
        assertEquals(1, savedFlags.getCaseFlags().size());

        FlagDetailsEntity flagDetail = savedFlags.getCaseFlags().iterator().next();
        assertEquals("FLAG_CODE_1", flagDetail.getFlagCode());
        assertEquals("Test Flag Comment 1", flagDetail.getFlagComment());
    }

    @Test
    void testMergePartyFlags_UpdateExistingPartyFlags() {
        UUID existingPartyId = UUID.randomUUID();

        FlagDetailsEntity existingFlagDetail = FlagDetailsEntity.builder()
            .flagCode("OLD_FLAG")
            .flagComment("Old Flag Comment")
            .build();

        FlagsEntity existingFlagsEntity = FlagsEntity.builder()
            .visibility("Hidden")
            .caseFlags(new ArrayList<>(List.of(existingFlagDetail)))
            .build();

        PartyEntity existingParty = PartyEntity.builder()
            .id(existingPartyId)
            .appellantFlags(new ArrayList<>(List.of(existingFlagsEntity)))
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(new HashSet<>(List.of(existingParty)))
            .build();

        Flags updatedFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(List.of(createFlagDetail("NEW_FLAG", "Updated Flag Comment")))
            .build();

        Party incomingParty = Party.builder().appellantFlags(updatedFlags).build();
        List<ListValue<Party>> incomingParties = List.of(createPartyListValue(
            existingPartyId.toString(),
            incomingParty
        ));

        caseFlagService.mergePartyFlags(incomingParties, pcsCaseEntity);

        assertEquals(1, pcsCaseEntity.getParties().size());
        PartyEntity updatedParty = pcsCaseEntity.getParties().iterator().next();

        assertNotNull(updatedParty.getAppellantFlags());
        assertEquals(1, updatedParty.getAppellantFlags().size());

        FlagsEntity updatedFlagsEntity = updatedParty.getAppellantFlags().get(0);
        assertEquals("Internal", updatedFlagsEntity.getVisibility());
        assertEquals(1, updatedFlagsEntity.getCaseFlags().size());

        FlagDetailsEntity updatedFlagDetail = updatedFlagsEntity.getCaseFlags().iterator().next();
        assertEquals("NEW_FLAG", updatedFlagDetail.getFlagCode());
        assertEquals("Updated Flag Comment", updatedFlagDetail.getFlagComment());
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

        caseFlagService.mergePartyFlags(new ArrayList<>(), pcsCaseEntity);

        assertEquals(1, pcsCaseEntity.getParties().size());
        PartyEntity retainedParty = pcsCaseEntity.getParties().iterator().next();

        assertEquals("John", retainedParty.getFirstName());
        assertEquals("Doe", retainedParty.getLastName());
        assertTrue(retainedParty.getAppellantFlags().isEmpty());
    }

    private PcsCaseEntity createPcsCaseEntity(UUID id) {
        return PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseReference(1234L)
            .caseFlags(createFlagDetailsEntity(id))
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

    private List<FlagDetailsEntity> createFlagDetailsEntity(UUID id) {
        List<FlagDetailsEntity> flagDetailsEntities = new ArrayList<>();

        FlagDetailsEntity flagDetailsEntity = FlagDetailsEntity.builder()
            .id(id)
            .defaultStatus("Active")
            .flagCode("CF0008")
            .name("Power of arrest with Police")
            .flagComment("Police arrest inactive")
            .availableExternally(YesOrNoConverter.toBoolean(YesOrNo.NO))
            .hearingRelevant(YesOrNoConverter.toBoolean(YesOrNo.YES))
            .paths(createFlagPathEntity())
            .dateTimeCreated(LocalDateTime.now())
            .build();
        flagDetailsEntities.add(flagDetailsEntity);

        return flagDetailsEntities;
    }

    private List<FlagPathEntity> createFlagPathEntity() {
        List<FlagPathEntity> flagPathEntities = new ArrayList<>();

        FlagPathEntity flagPathEntity = FlagPathEntity.builder()
            .id(UUID.randomUUID())
            .path("Case")
            .build();
        flagPathEntities.add(flagPathEntity);

        return flagPathEntities;
    }

    private ListValue<Party> createPartyListValue(String id, Party party) {
        return ListValue.<Party>builder()
            .id(id)
            .value(party)
            .build();
    }
}
