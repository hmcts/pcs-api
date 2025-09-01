package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.UnsubmittedCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.UnsubmittedCaseDataRepository;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnsubmittedCaseDataServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private UnsubmittedCaseDataRepository unsubmittedCaseDataRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Captor
    private ArgumentCaptor<UnsubmittedCaseDataEntity> unsubmittedCaseDataEntityCaptor;

    private UnsubmittedCaseDataService underTest;

    @BeforeEach
    void setUp() {
        underTest = new UnsubmittedCaseDataService(unsubmittedCaseDataRepository, objectMapper);
    }

    @Test
    void shouldGetUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String unsubmittedCaseDataJson = "case data json";
        UnsubmittedCaseDataEntity unsubmittedCaseDataEntity = mock(UnsubmittedCaseDataEntity.class);
        PCSCase expectedUnsubmittedCaseData = mock(PCSCase.class);

        when(unsubmittedCaseDataRepository.findByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(unsubmittedCaseDataEntity));
        when(unsubmittedCaseDataEntity.getCaseData()).thenReturn(unsubmittedCaseDataJson);
        when(objectMapper.readValue(unsubmittedCaseDataJson, PCSCase.class)).thenReturn(expectedUnsubmittedCaseData);

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE);

        // Then
        assertThat(unsubmittedCaseData).contains(expectedUnsubmittedCaseData);
        verify(expectedUnsubmittedCaseData).setHasUnsubmittedCaseData(YesOrNo.YES);
    }

    @Test
    void shouldReturnEmptyWhenNoUnsubmittedCaseData() {
        // Given
        when(unsubmittedCaseDataRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE);

        // Then
        assertThat(unsubmittedCaseData).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnWhetherUnsubmittedCaseDataExists(boolean repositoryDataExists) {
        // Given
        when(unsubmittedCaseDataRepository.existsByCaseReference(CASE_REFERENCE)).thenReturn(repositoryDataExists);

        // When
        boolean hasUnsubmittedCaseData = underTest.hasUnsubmittedCaseData(CASE_REFERENCE);

        // Then
        assertThat(hasUnsubmittedCaseData).isEqualTo(repositoryDataExists);
    }

    @Test
    void shouldSaveNewUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String caseDataJson = "case data json";
        PCSCase caseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(caseData)).thenReturn(caseDataJson);
        when(unsubmittedCaseDataRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        // When
        underTest.saveUnsubmittedCaseData(CASE_REFERENCE, caseData);

        // Then
        verify(unsubmittedCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        UnsubmittedCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(savedEntity.getCaseData()).isEqualTo(caseDataJson);
    }

    @Test
    void shouldUpdateExistingUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String caseDataJson = "case data json";
        PCSCase caseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(caseData)).thenReturn(caseDataJson);

        UnsubmittedCaseDataEntity unsubmittedCaseDataEntity = mock(UnsubmittedCaseDataEntity.class);
        when(unsubmittedCaseDataRepository.findByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(unsubmittedCaseDataEntity));

        // When
        underTest.saveUnsubmittedCaseData(CASE_REFERENCE, caseData);

        // Then
        verify(unsubmittedCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        UnsubmittedCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity).isSameAs(unsubmittedCaseDataEntity);
        verify(unsubmittedCaseDataEntity).setCaseData(caseDataJson);
    }

    @Test
    void shouldThrowExceptionForJsonExceptionWhenReading() throws JsonProcessingException {
        // Given
        String unsubmittedCaseDataJson = "case data json";
        UnsubmittedCaseDataEntity unsubmittedCaseDataEntity = mock(UnsubmittedCaseDataEntity.class);

        when(unsubmittedCaseDataRepository.findByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(unsubmittedCaseDataEntity));
        when(unsubmittedCaseDataEntity.getCaseData()).thenReturn(unsubmittedCaseDataJson);

        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);
        when(objectMapper.readValue(unsubmittedCaseDataJson, PCSCase.class)).thenThrow(jsonProcessingException);

        // When
        Throwable throwable = catchThrowable(() -> underTest.getUnsubmittedCaseData(CASE_REFERENCE));

        // Then
        assertThat(throwable)
            .isInstanceOf(UnsubmittedDataException.class)
            .hasMessage("Failed to read saved answers")
            .hasCause(jsonProcessingException);
    }

    @Test
    void shouldThrowExceptionForJsonExceptionWhenSaving() throws JsonProcessingException {
        // Given
        PCSCase caseData = mock(PCSCase.class);
        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);

        when(objectMapper.writeValueAsString(caseData)).thenThrow(jsonProcessingException);

        // When
        Throwable throwable = catchThrowable(() -> underTest.saveUnsubmittedCaseData(CASE_REFERENCE, caseData));

        // Then
        assertThat(throwable)
            .isInstanceOf(UnsubmittedDataException.class)
            .hasMessage("Failed to save answers")
            .hasCause(jsonProcessingException);
    }
}
