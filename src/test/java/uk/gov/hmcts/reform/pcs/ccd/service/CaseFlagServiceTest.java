package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CaseFlagServiceTest {

    private final CaseFlagService caseFlagService = new CaseFlagService();

    @Test
    void testMergeCaseFlags_NewCaseFlags() {
        FlagDetailsEntity existingFlagDetail = FlagDetailsEntity.builder()
            .flagCode("EXISTING_FLAG")
            .flagComment("Existing Case Flag Comment")
            .build();

        FlagsEntity existingFlagsEntity = FlagsEntity.builder()
            .id(UUID.randomUUID())
            .visibility(FlagVisibility.INTERNAL.getValue())
            .caseFlags(new ArrayList<>(List.of(existingFlagDetail)))
            .build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(List.of(createFlagDetail("FLAG_CODE_1", "Test Flag Comment 1")))
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseFlags(existingFlagsEntity).build();

        caseFlagService.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertNotNull(pcsCaseEntity.getCaseFlags());
        FlagsEntity savedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("Internal", savedFlags.getVisibility());
        assertEquals(1, savedFlags.getCaseFlags().size());
    }

    @Test
    void testMergeCaseFlags_UpdateExistingCaseFlags() {
        FlagDetailsEntity existingFlagDetail = FlagDetailsEntity.builder()
            .flagCode("EXISTING_FLAG")
            .flagComment("Existing Case Flag Comment")
            .build();

        FlagsEntity existingFlagsEntity = FlagsEntity.builder()
            .id(UUID.randomUUID())
            .visibility(FlagVisibility.INTERNAL.getValue())
            .caseFlags(new ArrayList<>(List.of(existingFlagDetail)))
            .build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(new ArrayList<>()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseFlags(existingFlagsEntity).build();

        caseFlagService.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertNotNull(pcsCaseEntity.getCaseFlags());
        FlagsEntity updatedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("Internal", updatedFlags.getVisibility());
        assertEquals(0, updatedFlags.getCaseFlags().size());
    }

    @Test
    void testMergeCaseFlags_NoIncomingCaseFlags() {
        FlagDetailsEntity existingFlagDetail = FlagDetailsEntity.builder()
            .flagCode("EXISTING_FLAG")
            .flagComment("Existing Case Flag Comment")
            .build();

        FlagsEntity existingFlagsEntity = FlagsEntity.builder()
            .id(UUID.randomUUID())
            .visibility(FlagVisibility.INTERNAL.getValue())
            .caseFlags(new ArrayList<>(List.of(existingFlagDetail)))
            .build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(new ArrayList<>()).build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseFlags(existingFlagsEntity).build();

        caseFlagService.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertNotNull(pcsCaseEntity.getCaseFlags());
        FlagsEntity unchangedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("Internal", unchangedFlags.getVisibility());
        assertEquals(0, unchangedFlags.getCaseFlags().size());
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

    private ListValue<FlagDetail> createFlagDetail(String flagCode, String comment) {
        return ListValue.<FlagDetail>builder()
                .id(UUID.randomUUID().toString())
                .value(FlagDetail.builder().flagCode(flagCode).flagComment(comment).build())
                .build();
    }

    private ListValue<Party> createPartyListValue(String id, Party party) {
        return ListValue.<Party>builder()
                .id(id)
                .value(party)
                .build();
    }
}
