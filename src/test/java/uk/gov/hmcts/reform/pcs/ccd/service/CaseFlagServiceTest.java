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
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagPathEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.RefDataFlagsRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class CaseFlagServiceTest {

    @Mock
    private RefDataFlagsRepository refDataFlagsRepository;

    @InjectMocks
    private CaseFlagService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CaseFlagService(refDataFlagsRepository);
    }

    @Test
    void shouldMergeNewCaseFlags() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null,"CF0002", "Complex Case",
                                              "Complicated case", "Active", false))
            .build();

        // When
        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        // Then
        assertNotNull(pcsCaseEntity.getCaseFlags());
        List<FlagDetailsEntity> savedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals("CF0002", savedFlags.getFirst().getFlagCode());
        assertEquals("Complicated case", savedFlags.getFirst().getFlagComment());
        assertEquals("Active", savedFlags.getFirst().getDefaultStatus());
        assertEquals(1, savedFlags.size());

        List<FlagPathEntity> savedPaths = savedFlags.getFirst().getPaths();
        assertEquals(1, savedPaths.size());
        assertEquals("Case", savedPaths.getFirst().getPath());
    }

    @Test
    void shouldMergeNewCaseFlagsWithNoPaths() {
        // Given
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(createFlagDetail(null,"CF0007", "Urgent case",
                                      "Urgent case test", "Active", true))
            .build();

        // When
        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity);

        // Then
        List<FlagDetailsEntity> savedFlags = pcsCaseEntity.getCaseFlags();
        List<FlagPathEntity> savedPaths = savedFlags.getFirst().getPaths();
        assertEquals("Active", savedFlags.getFirst().getDefaultStatus());
        assertEquals(1, savedFlags.size());
        assertEquals(0, savedPaths.size());


    }

    @Test
    void shouldAmendExistingCaseFlags() {
        // Given
        UUID id = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity(id);
        List<ListValue<FlagDetail>> flagDetails = new ArrayList<>();
        flagDetails.addAll(createFlagDetail(null, "CF0002", "Complex Case",
                                            "Complicated case", "Active", false
        ));
        flagDetails.addAll(createFlagDetail(id.toString(),"CF0008", "Power of arrest with Police ",
                                            "Police arrest inactive", "Inactive", false));
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
        assertEquals(2, savedFlags.size());
        assertThat(savedFlags).extracting(FlagDetailsEntity::getFlagCode).containsExactly("CF0002", "CF0008");
        assertThat(savedFlags.getLast().getFlagComment()).isEqualTo("Police arrest inactive");
        assertThat(savedFlags.getLast().getFlagCode()).isEqualTo("CF0008");
    }

    private PcsCaseEntity createPcsCaseEntity(UUID id) {
        return PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseReference(1234L)
            .caseFlags(createFlagDetailsEntity(id))
            .build();
    }

    private List<ListValue<FlagDetail>> createFlagDetail(String id, String flagCode, String name,
                                                         String flagComment, String status, boolean isPathEmpty) {
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
                       .path(createPathListValue(isPathEmpty))
                       .build())
            .build();
        flagDetails.add(flagDetailListValue);

        return flagDetails;
    }

    private List<ListValue<String>> createPathListValue(boolean isPathEmpty) {
        List<ListValue<String>> paths = new ArrayList<>();

        ListValue<String> path = ListValue.<String>builder()
            .id(UUID.randomUUID().toString())
            .value("Case")
            .build();
        paths.add(path);

        return isPathEmpty ? null : paths;
    }

    private List<FlagDetailsEntity> createFlagDetailsEntity(UUID id) {
        List<FlagDetailsEntity> flagDetailsEntities = new ArrayList<>();

        FlagDetailsEntity flagDetailsEntity = FlagDetailsEntity.builder()
            .id(id)
            .defaultStatus("Active")
            .flagCode("CF0008")
            .flagComment("Police arrest inactive")
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
}
