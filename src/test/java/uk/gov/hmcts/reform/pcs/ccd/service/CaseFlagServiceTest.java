package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;

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
        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(List.of(createFlagDetail()))
            .build();

        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        assertNotNull(pcsCaseEntity.getCaseFlags());
        List<FlagsEntity> savedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("Internal", savedFlags.getFirst().getVisibility());
        assertEquals(1, savedFlags.getFirst().getFlagDetails().size());
    }

    private ListValue<FlagDetail> createFlagDetail() {
        return ListValue.<FlagDetail>builder()
            .id(UUID.randomUUID().toString())
            .value(FlagDetail.builder()
                       .flagCode("RA005")
                       .flagComment("Communication")
                       .availableExternally(YesOrNo.NO)
                       .hearingRelevant(YesOrNo.YES)
                       .build())
            .build();
    }
}
