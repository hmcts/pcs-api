package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class CaseFlagServiceTest {

    private CaseFlagService underTest;
    private PcsCaseEntity pcsCaseEntity;

    @BeforeEach
    void setUp() {
        underTest = new CaseFlagService();
        pcsCaseEntity = PcsCaseEntity.builder().build();
    }

    @Test
    void shouldMergeNewCaseFlags() {
        /*FlagDetailsEntity existingFlagDetail = FlagDetailsEntity.builder()
            .flagCode("EXISTING_FLAG")
            .flagComment("Existing Case Flag Comment")
            .build();

        FlagsEntity existingFlagsEntity = FlagsEntity.builder()
            .id(UUID.randomUUID())
            .visibility(FlagVisibility.INTERNAL.getValue())
            .flagDetails(new ArrayList<>(List.of(existingFlagDetail)))
            .build();*/

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(List.of(createFlagDetail()))
            .build();

       /* PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseFlags(List.of(existingFlagsEntity)).build();*/

        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertNotNull(pcsCaseEntity.getCaseFlags());
        List<FlagsEntity> savedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("Internal", savedFlags.getFirst().getVisibility());
        assertEquals(1, savedFlags.getFirst().getFlagDetails().size());
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
            .flagDetails(List.of(existingFlagDetail))
            .build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(new ArrayList<>()).build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            //.id(UUID.randomUUID())
            //.caseFlags(List.of(existingFlagsEntity))
            .build();

        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertNotNull(pcsCaseEntity.getCaseFlags());
        List<FlagsEntity> updatedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("Internal", updatedFlags.getFirst().getVisibility());
        assertEquals(0, updatedFlags.getFirst().getFlagDetails().size());
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
            .flagDetails(List.of(existingFlagDetail))
            .build();

        FlagDetail flagDetail  = FlagDetail.builder()
            .flagCode("EXISTING_FLAG")
            .build();
        List<ListValue<FlagDetail>> flagDetails =     List.of(
            ListValue.<FlagDetail>builder()
                .id(UUID.randomUUID().toString())
                .value(flagDetail)
                .build()
        );

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(flagDetails)
            .build();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseFlags(List.of(existingFlagsEntity))
            .build();

        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertNotNull(pcsCaseEntity.getCaseFlags());
        List<FlagsEntity> unchangedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("Internal", unchangedFlags.getFirst().getVisibility());
        assertEquals(0, unchangedFlags.getFirst().getFlagDetails().size());
    }

    private ListValue<FlagDetail> createFlagDetail() {
        return ListValue.<FlagDetail>builder()
            .id(UUID.randomUUID().toString())
            .value(FlagDetail.builder().flagCode("RA005").flagComment("Communication").build())
            .build();
    }

    private ListValue<Party> createPartyListValue(String id, Party party) {
        return ListValue.<Party>builder()
            .id(id)
            .value(party)
            .build();
    }
}
