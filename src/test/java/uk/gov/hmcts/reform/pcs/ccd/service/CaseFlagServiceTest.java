package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventFlow;
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;

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
        // Given
        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(List.of(createFlagDetail()))
            .build();

        /*pcsCaseEntity.setId(UUID.randomUUID());
        pcsCaseEntity.setCaseReference(1234L);
        pcsCaseEntity.setCaseFlags(List.of(createFlagDetailsEntity()));*/

        // When
        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity, EventFlow.CREATE.name());

        // Then
        assertNotNull(pcsCaseEntity.getCaseFlags());
        List<FlagDetailsEntity> savedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("CF0002", savedFlags.getFirst().getFlagCode());
        assertEquals("Complicated case", savedFlags.getFirst().getFlagComment());
        assertEquals("Complex Case", savedFlags.getFirst().getName());
        assertEquals(1, savedFlags.size());
    }

    private ListValue<FlagDetail> createFlagDetail() {
        return ListValue.<FlagDetail>builder()
            .id(UUID.randomUUID().toString())
            .value(FlagDetail.builder()
                       .flagCode("CF0002")
                       .name("Complex Case")
                       .flagComment("Complicated case")
                       .availableExternally(YesOrNo.NO)
                       .hearingRelevant(YesOrNo.YES)
                       .path(List.of(createPathListValue()))
                       .build())
            .build();
    }

    private ListValue<String> createPathListValue() {
        return ListValue.<String>builder()
            .id(UUID.randomUUID().toString())
            .value("Case")
            .build();
    }

    private FlagDetailsEntity createFlagDetailsEntity() {
        return FlagDetailsEntity.builder()
            .id(UUID.randomUUID())
            .flagCode("CF0008")
            .name("Power of arrest with Police")
            .availableExternally(YesOrNoConverter.toBoolean(YesOrNo.NO))
            .hearingRelevant(YesOrNoConverter.toBoolean(YesOrNo.YES))
            .paths(List.of(createFlagPathEntity()))
            .build();
    }

    private FlagPathEntity createFlagPathEntity() {
        return FlagPathEntity.builder()
            .id(UUID.randomUUID())
            .path("Case")
            .build();
    }
}
