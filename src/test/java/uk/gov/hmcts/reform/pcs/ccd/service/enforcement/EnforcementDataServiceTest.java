package uk.gov.hmcts.reform.pcs.ccd.service.enforcement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcement.EnforcementDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcement.EnforcementDataRepository;
import uk.gov.hmcts.reform.pcs.exception.JsonReaderException;
import uk.gov.hmcts.reform.pcs.exception.JsonWriterException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementDataServiceTest {

    @Mock
    private EnforcementDataRepository enfDataRepository;

    @Mock
    private PcsCaseRepository pcsCaseRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<EnforcementDataEntity> enfDataEntityCaptor;

    private EnforcementDataService enfDataService;

    private UUID enforcementCaseId;

    private UUID pcsCaseId;

    static final long CASE_REFERENCE = 1234L;

    @BeforeEach
    void setUp() {
        enfDataService = new EnforcementDataService(enfDataRepository, pcsCaseRepository, objectMapper);
        enforcementCaseId = UUID.randomUUID();
        pcsCaseId = UUID.randomUUID();
    }

    @Test
    void shouldReturnEmptyWhenNoSubmittedEnforcementData() {
        // Given
        when(enfDataRepository.findById(enforcementCaseId)).thenReturn(Optional.empty());

        // When
        Optional<EnforcementOrder> savedSubmittedEnfData =
                enfDataService.retrieveSubmittedEnforcementData(enforcementCaseId);

        // Then
        assertThat(savedSubmittedEnfData).isEmpty();
    }

    @Test
    void shouldReturnSubmittedEnforcementDataWhenFound() {
        // Given
        EnforcementDataEntity enforcementDataEntity = EnforcementDataUtil.buildSampleEnforcementDataEntity(
                enforcementCaseId, pcsCaseId);
        when(enfDataRepository.findById(enforcementCaseId)).thenReturn(Optional.of(enforcementDataEntity));

        // When
        EnforcementOrder submittedEnforcementOrder =
                enfDataService.retrieveSubmittedEnforcementData(enforcementCaseId).orElse(null);

        // Then
        assertThat(submittedEnforcementOrder).isEqualTo(EnforcementDataUtil.buildSampleEnforcementData());
    }

    @Test
    void shouldThrowJsonReaderExceptionWhenSubmittedEnforcementDataIsMalformed() {
        // Given
        EnforcementDataEntity enforcementDataEntity = new EnforcementDataEntity();
        enforcementDataEntity.setId(enforcementCaseId);
        enforcementDataEntity.setEnforcementData("malformed json");
        when(enfDataRepository.findById(enforcementCaseId)).thenReturn(Optional.of(enforcementDataEntity));

        // When
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                enfDataService.retrieveSubmittedEnforcementData(enforcementCaseId));

        // Then
        assertThat(thrown).isInstanceOf(JsonReaderException.class)
                .hasMessageContaining("Failed to read submitted Enforcement data JSON");
    }

    @Test
    void shouldSaveNewSubmittedEnforcementData() {
        // Given
        EnforcementOrder enforcementData = EnforcementDataUtil.buildSampleEnforcementData();

        PCSCase pcsCase = mock(PCSCase.class);
        pcsCase.setEnforcementOrder(enforcementData);

        PcsCaseEntity pcsCaseEntity = EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId);

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
                .thenReturn(Optional.of(pcsCaseEntity));
        when(pcsCase.getEnforcementOrder()).thenReturn(enforcementData);

        // When
        enfDataService.createEnforcementData(CASE_REFERENCE, pcsCase);

        // Then
        verify(enfDataRepository).save(enfDataEntityCaptor.capture());

        EnforcementDataEntity savedEntity = enfDataEntityCaptor.getValue();
        String savedEnfDataJson = savedEntity.getEnforcementData();
        assertThat(JsonPath.parse(savedEnfDataJson)
                .read("$.selectEnforcementType", String.class)).isEqualTo("WARRANT");
        assertThat(JsonPath.parse(savedEnfDataJson)
                .read("$.anyRiskToBailiff", String.class)).isEqualTo("YES");
        assertThat(JsonPath.parse(savedEnfDataJson)
                .read("$.enforcementViolentDetails", String.class)).isEqualTo("Violent");
        assertThat(JsonPath.parse(savedEnfDataJson)
                .<List<String>>read("$.enforcementRiskCategories"))
                .containsExactlyInAnyOrder("VERBAL_OR_WRITTEN_THREATS", "VIOLENT_OR_AGGRESSIVE");
        assertThat(JsonPath.parse(savedEnfDataJson)
                .read("$.correctNameAndAddress", String.class)).isEqualTo("YES");
    }

    @Test
    void shouldThrowJsonWriterExceptionWhenSubmittedEnforcementDataCannotBeSerialized() {
        // Given
        PCSCase pcsCase = mock(PCSCase.class);
        EnforcementOrder enforcementData = null;
        pcsCase.setEnforcementOrder(enforcementData);

        EnforcementDataUtil.buildPcsCaseEntity(pcsCaseId);

        // Mock ObjectMapper to throw exception
        ObjectMapper mockObjectMapper = mock(ObjectMapper.class);
        enfDataService = new EnforcementDataService(enfDataRepository, pcsCaseRepository, mockObjectMapper);

        try {
            when(mockObjectMapper.writeValueAsString(enforcementData))
                    .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("Test exception") {
                    });
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // This block will not be executed
        }

        // When
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() ->
                enfDataService.createEnforcementData(CASE_REFERENCE, pcsCase));

        // Then
        assertThat(thrown).isInstanceOf(JsonWriterException.class)
                .hasMessageContaining("Failed to write submitted Enforcement data");
    }
}