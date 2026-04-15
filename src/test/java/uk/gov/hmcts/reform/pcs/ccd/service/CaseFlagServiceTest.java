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
import uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter;

import java.time.LocalDateTime;
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
        // Given
        UUID id = UUID.randomUUID();
        pcsCaseEntity.setId(id);
        pcsCaseEntity.setCaseReference(1234L);
        pcsCaseEntity.setCaseFlags(createFlagDetailsEntity());

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(List.of(createFlagDetail(null,"CF0002", "Complex Case", "Complicated case"),
                             createFlagDetail(id.toString(),"CF0008", "Power of arrest with Police",
                                              "Police arrest")))
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
    }

    private ListValue<FlagDetail> createFlagDetail(String id, String flagCode, String name, String flagComment) {
        return ListValue.<FlagDetail>builder()
            .id(id == null ? UUID.randomUUID().toString() : id)
            .value(FlagDetail.builder()
                       .flagCode(flagCode)
                       .name(name)
                       .flagComment(flagComment)
                       .status("Active")
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

    private List<FlagDetailsEntity> createFlagDetailsEntity() {
        List<FlagDetailsEntity> flagDetailsEntities = new ArrayList<>();

        FlagDetailsEntity flagDetailsEntity = FlagDetailsEntity.builder()
            .id(UUID.randomUUID())
            .flagCode("CF0008")
            .name("Power of arrest with Police")
            .availableExternally(YesOrNoConverter.toBoolean(YesOrNo.NO))
            .hearingRelevant(YesOrNoConverter.toBoolean(YesOrNo.YES))
            .paths(List.of(createFlagPathEntity()))
            .dateTimeCreated(LocalDateTime.now())
            .build();
        flagDetailsEntities.add(flagDetailsEntity);

        return flagDetailsEntities;
    }

    private FlagPathEntity createFlagPathEntity() {
        return FlagPathEntity.builder()
            .id(UUID.randomUUID())
            .path("Case")
            .build();
    }
}
