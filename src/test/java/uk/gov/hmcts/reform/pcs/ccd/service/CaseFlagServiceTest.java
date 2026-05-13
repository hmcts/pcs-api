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
import uk.gov.hmcts.reform.pcs.ccd.entity.CaseFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.BaseCaseFlag;
import uk.gov.hmcts.reform.pcs.ccd.entity.FlagRefDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventFlow;
import uk.gov.hmcts.reform.pcs.ccd.repository.FlagRefDataRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
                                              "Complicated case", "Active", false))
            .build();

        // When
        List<BaseCaseFlag> savedFlags = underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity, EventFlow.CREATE.name());

        // Then
        assertEquals("CF0002", savedFlags.getFirst().getFlagRefData().getFlagCode());
        assertEquals("Complicated case", savedFlags.getFirst().getFlagComment());
        assertEquals("Active", savedFlags.getFirst().getDefaultStatus());
        assertEquals(1, savedFlags.size());

        String savedPaths = Arrays.stream(savedFlags.getFirst().getPaths().split(":")).toList().getLast();
        assertNotNull(savedPaths);
        assertEquals("Case", savedPaths);
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
        List<BaseCaseFlag> savedFlags = underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity, EventFlow.CREATE.name());

        // Then
        String savedPaths = savedFlags.getFirst().getPaths();
        assertEquals("Active", savedFlags.getFirst().getDefaultStatus());
        assertEquals(1, savedFlags.size());
        assertThat(savedPaths).isNull();
    }

    @Test
    void shouldAmendExistingCaseFlags() {
        // Given
        UUID id = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity(id);
        List<ListValue<FlagDetail>> flagDetails = new ArrayList<>();
        flagDetails.addAll(createFlagDetail(id.toString(),"CF0008", "Power of arrest with Police ",
                                            "Police arrest inactive", "Inactive", false));
        Flags incomingFlags = Flags.builder()
            .visibility(FlagVisibility.INTERNAL)
            .details(flagDetails)
            .build();

        // When
        underTest.mergeCaseFlags(incomingFlags, pcsCaseEntity, EventFlow.UPDATE.name());

        // Then
        assertNotNull(pcsCaseEntity.getCaseFlags());
        List<BaseCaseFlag> savedFlags = pcsCaseEntity.getCaseFlags();
        assertEquals(1, savedFlags.size());
        assertThat(savedFlags.getLast().getFlagComment()).isEqualTo("Police arrest inactive");
        assertThat(savedFlags.getLast().getFlagRefData().getFlagCode()).isEqualTo("CF0008");
    }

    private PcsCaseEntity createPcsCaseEntity(UUID id) {
        return PcsCaseEntity.builder()
            .id(UUID.randomUUID())
            .caseReference(1234L)
            .caseFlags(createCaseFlagEntity(id))
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

    private List<BaseCaseFlag> createCaseFlagEntity(UUID id) {


        FlagRefDataEntity flagRefDataEntity = new FlagRefDataEntity();
        BaseCaseFlag caseFlagEntity = new CaseFlagEntity();
        caseFlagEntity.setFlagRefData(flagRefDataEntity);
        caseFlagEntity.setId(id);
        caseFlagEntity.setDefaultStatus("Active");
        caseFlagEntity.getFlagRefData().setFlagCode("CF0008");
        caseFlagEntity.setFlagComment("Police arrest inactive");
        caseFlagEntity.setPaths("Case");
        caseFlagEntity.setDateTimeModified(LocalDateTime.now());

        List<BaseCaseFlag> caseFlagEntities = new ArrayList<>();
        caseFlagEntities.add(caseFlagEntity);

        return caseFlagEntities;
    }
}
