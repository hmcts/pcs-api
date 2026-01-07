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
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftCaseDataEntity;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftCaseDataRepository;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftCaseDataServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private DraftCaseDataRepository draftCaseDataRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private DraftCaseJsonMerger draftCaseJsonMerger;
    @Captor
    private ArgumentCaptor<DraftCaseDataEntity> unsubmittedCaseDataEntityCaptor;

    private final EventId eventId = EventId.resumePossessionClaim;

    private DraftCaseDataService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DraftCaseDataService(draftCaseDataRepository, objectMapper, draftCaseJsonMerger);
    }

    @Test
    void shouldGetUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String unsubmittedCaseDataJson = "case data json";
        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);
        PCSCase expectedUnsubmittedCaseData = mock(PCSCase.class);

        when(draftCaseDataRepository.findByCaseReferenceAndEventId(CASE_REFERENCE, eventId))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataEntity.getCaseData()).thenReturn(unsubmittedCaseDataJson);
        when(objectMapper.readValue(unsubmittedCaseDataJson, PCSCase.class)).thenReturn(expectedUnsubmittedCaseData);

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        assertThat(unsubmittedCaseData).contains(expectedUnsubmittedCaseData);
        verify(expectedUnsubmittedCaseData).setHasUnsubmittedCaseData(YesOrNo.YES);
    }

    @Test
    void shouldReturnEmptyWhenNoUnsubmittedCaseData() {
        // Given
        when(draftCaseDataRepository.findByCaseReferenceAndEventId(CASE_REFERENCE, eventId))
            .thenReturn(Optional.empty());

        // When
        Optional<PCSCase> unsubmittedCaseData = underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        assertThat(unsubmittedCaseData).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnWhetherUnsubmittedCaseDataExists(boolean repositoryDataExists) {
        // Given
        when(draftCaseDataRepository.existsByCaseReferenceAndEventId(CASE_REFERENCE, eventId))
            .thenReturn(repositoryDataExists);

        // When
        boolean hasUnsubmittedCaseData = underTest.hasUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        assertThat(hasUnsubmittedCaseData).isEqualTo(repositoryDataExists);
    }

    @Test
    void shouldSaveNewUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String caseDataJson = "case data json";
        PCSCase caseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(caseData)).thenReturn(caseDataJson);
        when(draftCaseDataRepository.findByCaseReferenceAndEventId(CASE_REFERENCE, eventId))
            .thenReturn(Optional.empty());

        // When
        underTest.patchUnsubmittedEventData(CASE_REFERENCE, caseData, eventId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(savedEntity.getCaseData()).isEqualTo(caseDataJson);
    }

    @Test
    void shouldUpdateExistingUnsubmittedCaseData() throws JsonProcessingException {
        // Given
        String existingCaseDataJson = "existing case data json";
        String newCaseDataJson = "new case data json";
        String mergedCaseDataJson = "merged case data JSON";

        PCSCase newCaseData = mock(PCSCase.class);
        when(objectMapper.writeValueAsString(newCaseData)).thenReturn(newCaseDataJson);

        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);
        when(draftCaseDataEntity.getCaseData()).thenReturn(existingCaseDataJson);

        when(draftCaseJsonMerger.mergeJson(existingCaseDataJson, newCaseDataJson)).thenReturn(mergedCaseDataJson);

        when(draftCaseDataRepository.findByCaseReferenceAndEventId(CASE_REFERENCE, eventId))
            .thenReturn(Optional.of(draftCaseDataEntity));

        // When
        underTest.patchUnsubmittedEventData(CASE_REFERENCE, newCaseData, eventId);

        // Then
        verify(draftCaseDataRepository).save(unsubmittedCaseDataEntityCaptor.capture());
        DraftCaseDataEntity savedEntity = unsubmittedCaseDataEntityCaptor.getValue();

        assertThat(savedEntity).isSameAs(draftCaseDataEntity);
        verify(draftCaseDataEntity).setCaseData(mergedCaseDataJson);
    }

    @Test
    void shouldDeleteUnsubmittedDataByCaseReference() {
        // When
        underTest.deleteUnsubmittedCaseData(CASE_REFERENCE, eventId);

        // Then
        verify(draftCaseDataRepository).deleteByCaseReferenceAndEventId(CASE_REFERENCE, eventId);
    }

    @Test
    void shouldThrowExceptionForJsonExceptionWhenReading() throws JsonProcessingException {
        // Given
        String unsubmittedCaseDataJson = "case data json";
        DraftCaseDataEntity draftCaseDataEntity = mock(DraftCaseDataEntity.class);

        when(draftCaseDataRepository.findByCaseReferenceAndEventId(CASE_REFERENCE, eventId))
            .thenReturn(Optional.of(draftCaseDataEntity));
        when(draftCaseDataEntity.getCaseData()).thenReturn(unsubmittedCaseDataJson);

        JsonProcessingException jsonProcessingException = mock(JsonProcessingException.class);
        when(objectMapper.readValue(unsubmittedCaseDataJson, PCSCase.class)).thenThrow(jsonProcessingException);

        // Then
        assertThatThrownBy(() -> underTest.getUnsubmittedCaseData(CASE_REFERENCE, eventId))
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

        // Then
        assertThatThrownBy(() -> underTest.patchUnsubmittedEventData(CASE_REFERENCE, caseData, eventId))
           .isInstanceOf(UnsubmittedDataException.class)
            .hasMessage("Failed to save answers")
            .hasCause(jsonProcessingException);

    }
}
