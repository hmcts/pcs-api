package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.DeletionCaseData;
import uk.gov.hmcts.reform.pcs.ccd.repository.CcdCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.exception.CcdCaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcdCaseDataDeletionServiceTest {

    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CcdCaseRepository ccdCaseRepository;

    @Captor
    private ArgumentCaptor<CaseDataContent> caseDataContentCaptor;

    private CcdCaseDataDeletionService underTest;

    private static final int DISCARD_AFTER_DAYS = 30;
    private static final long CASE_REF_1 = 123456789L;
    private static final long CASE_REF_2 = 987654321L;
    private static final String CASE_1 = "123456789";
    private static final String IDAM_TOKEN = "idam-token";
    private static final String SERVICE_AUTH = "service-auth";

    @BeforeEach
    void setUp() {
        underTest = new CcdCaseDataDeletionService(
                systemUpdateUserTokenProvider,
                authTokenGenerator,
                coreCaseDataApi,
                objectMapper,
                ccdCaseRepository
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoExpiredCasesFound() {
        // Given
        when(ccdCaseRepository.findExpiredDraftCases(DISCARD_AFTER_DAYS, 10)).thenReturn(List.of());

        // When
        List<DeletionCaseData> result = underTest.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, 10);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnValidListWhenExpiredCasesFound() {
        // Given
        when(ccdCaseRepository.findExpiredDraftCases(DISCARD_AFTER_DAYS, 10))
                .thenReturn(List.of(
                        DeletionCaseData.builder().caseRef(CASE_REF_1).state(State.PENDING_CASE_ISSUED).build(),
                        DeletionCaseData.builder().caseRef(CASE_REF_2).state(State.DRAFT_DISCARDED).build()
                ));

        // When
        List<DeletionCaseData> result = underTest.findExpiredDraftCasesBatch(DISCARD_AFTER_DAYS, 10);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldInvokeDeleteCcdCaseData() {
        // Given & When
        underTest.deleteCcdCaseData(CASE_REF_1);

        // Then
        verify(ccdCaseRepository).deleteCcdCaseData(CASE_REF_1);
    }

    @Test
    void shouldMarkCaseForDeletionWithCorrectEventId() throws Exception {
        // Given

        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();
        CaseResource caseResource = new CaseResource();
        caseResource.setReference(CASE_1);

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(IDAM_TOKEN);
        when(coreCaseDataApi.startEvent(IDAM_TOKEN, SERVICE_AUTH, CASE_1, EventId.markCaseForDeletion.name()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(eq(IDAM_TOKEN), eq(SERVICE_AUTH), eq(CASE_1), any(CaseDataContent.class)))
                .thenReturn(caseResource);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When
        CaseResource result = underTest.markCaseForDeletion(CASE_REF_1);

        // Then
        assertThat(result).isEqualTo(caseResource);
        verify(coreCaseDataApi).startEvent(IDAM_TOKEN, SERVICE_AUTH, CASE_1, EventId.markCaseForDeletion.name());
        verify(coreCaseDataApi).createEvent(eq(IDAM_TOKEN), eq(SERVICE_AUTH), eq(CASE_1),
                caseDataContentCaptor.capture());

        CaseDataContent capturedContent = caseDataContentCaptor.getValue();
        assertThat(capturedContent.getEvent().getId()).isEqualTo(EventId.markCaseForDeletion.name());
        assertThat(capturedContent.getEventToken()).isEqualTo("event-token");
    }

    @Test
    void shouldConfirmCaseDisposalWithCorrectEventId() throws Exception {
        // Given
        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();
        CaseResource caseResource = new CaseResource();
        caseResource.setReference(CASE_1);

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(IDAM_TOKEN);
        when(coreCaseDataApi.startEvent(IDAM_TOKEN, SERVICE_AUTH, CASE_1, EventId.confirmCaseDisposal.name()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(eq(IDAM_TOKEN), eq(SERVICE_AUTH), eq(CASE_1), any(CaseDataContent.class)))
                .thenReturn(caseResource);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When
        CaseResource result = underTest.confirmCaseDisposal(CASE_REF_1);

        // Then
        assertThat(result).isEqualTo(caseResource);
        verify(coreCaseDataApi).startEvent(IDAM_TOKEN, SERVICE_AUTH, CASE_1, EventId.confirmCaseDisposal.name());
        verify(coreCaseDataApi).createEvent(eq(IDAM_TOKEN), eq(SERVICE_AUTH), eq(CASE_1),
                caseDataContentCaptor.capture());

        CaseDataContent capturedContent = caseDataContentCaptor.getValue();
        assertThat(capturedContent.getEvent().getId()).isEqualTo(EventId.confirmCaseDisposal.name());
        assertThat(capturedContent.getEventToken()).isEqualTo("event-token");
    }

    @Test
    void shouldHandleFeignExceptionWhenPerformingEvent() throws Exception {
        // Given
        String errorMsg = "Failed to create event.";
        final FeignException feignException = createMockFeignException(errorMsg);

        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(IDAM_TOKEN);
        when(coreCaseDataApi.startEvent(IDAM_TOKEN, SERVICE_AUTH, CASE_1, EventId.markCaseForDeletion.name()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(eq(IDAM_TOKEN), eq(SERVICE_AUTH), eq(CASE_1), any(CaseDataContent.class)))
                .thenThrow(feignException);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When & Then
        assertThrows(FeignException.class, () -> underTest.markCaseForDeletion(CASE_REF_1));

    }

    @Test
    void shouldThrowCcdCaseNotFoundExceptionIfCaseIDFoundToBeInvalid() throws Exception {
        // Given
        String errorMsg = "Case ID is not valid";
        final FeignException feignException = createMockFeignException(errorMsg);

        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(IDAM_TOKEN);
        when(coreCaseDataApi.startEvent(IDAM_TOKEN, SERVICE_AUTH, CASE_1, EventId.markCaseForDeletion.name()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(eq(IDAM_TOKEN), eq(SERVICE_AUTH), eq(CASE_1), any(CaseDataContent.class)))
                .thenThrow(feignException);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When & Then
        assertThrows(CcdCaseNotFoundException.class, () -> underTest.markCaseForDeletion(CASE_REF_1));
    }

    @Test
    void shouldThrowCcdCaseNotFoundExceptionIfNoCaseFoundForReference() throws Exception {
        // Given
        String errorMsg = "No case found for reference";
        final FeignException feignException = createMockFeignException(errorMsg);

        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();

        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTH);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(IDAM_TOKEN);
        when(coreCaseDataApi.startEvent(IDAM_TOKEN, SERVICE_AUTH, CASE_1, EventId.markCaseForDeletion.name()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(eq(IDAM_TOKEN), eq(SERVICE_AUTH), eq(CASE_1), any(CaseDataContent.class)))
                .thenThrow(feignException);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When & Then
        assertThrows(CcdCaseNotFoundException.class, () -> underTest.markCaseForDeletion(CASE_REF_1));
    }

    private FeignException createMockFeignException(String message) {
        return FeignException.errorStatus(
                message,
                Response.builder()
                        .reason("Error reason")
                        .request(mock(Request.class))
                        .headers(Collections.emptyMap())
                        .body(new byte[0])
                        .build()
        );
    }
}
