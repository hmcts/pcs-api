package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.DraftCasesToDiscard;
import uk.gov.hmcts.reform.pcs.ccd.repository.CcdCaseRepository;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcdCaseDataServiceTest {

    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock
    private CcdCaseRepository ccdCaseRepository;

    @Captor
    private ArgumentCaptor<SqlParameterSource> sqlParameterSourceCaptor;

    @Captor
    private ArgumentCaptor<CaseDataContent> caseDataContentCaptor;

    private CcdCaseDataService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CcdCaseDataService(
                systemUpdateUserTokenProvider,
                authTokenGenerator,
                coreCaseDataApi,
                objectMapper,
                jdbcTemplate,
                ccdCaseRepository
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFindExpiredDraftCasesWithCorrectDiscardDaysParameter() {
        // Given
        int discardAfterDays = 7;
        List<DraftCasesToDiscard> expectedCases = List.of(
                DraftCasesToDiscard.builder().caseReference(123L).build(),
                DraftCasesToDiscard.builder().caseReference(456L).build()
        );

        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(expectedCases);

        // When
        List<DraftCasesToDiscard> result = underTest.findExpiredDraftCases(discardAfterDays);

        // Then
        assertThat(result).isEqualTo(expectedCases);
        verify(jdbcTemplate).query(anyString(), sqlParameterSourceCaptor.capture(), any(RowMapper.class));

        SqlParameterSource capturedParams = sqlParameterSourceCaptor.getValue();
        assertThat(capturedParams.getValue("discardDaysAfter")).isEqualTo(7);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenNoExpiredCasesFound() {
        // Given
        int discardAfterDays = 7;
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        // When
        List<DraftCasesToDiscard> result = underTest.findExpiredDraftCases(discardAfterDays);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapResultSetRowToDraftCasesToDiscard() throws SQLException {
        // Given
        int discardAfterDays = 7;
        long expectedCaseRef = 999999L;

        ResultSet mockResultSet = mock(ResultSet.class);
        when(mockResultSet.getLong("reference")).thenReturn(expectedCaseRef);

        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<DraftCasesToDiscard> rowMapper = invocation.getArgument(2);
                    DraftCasesToDiscard result = rowMapper.mapRow(mockResultSet, 0);
                    return List.of(result);
                });

        // When
        List<DraftCasesToDiscard> result = underTest.findExpiredDraftCases(discardAfterDays);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCaseReference()).isEqualTo(expectedCaseRef);
        verify(mockResultSet).getLong("reference");
    }

    @Test
    void shouldInvokeDeleteCcdCaseData() {
        // Given
        long caseReference = 12345L;

        // When
        underTest.deleteCcdCaseData(caseReference);

        // Then
        verify(ccdCaseRepository).deleteCcdCaseData(caseReference);

    }

    @Test
    void shouldMarkCaseForDeletionWithCorrectEventId() throws Exception {
        // Given
        final long caseRef = 12345L;
        String idamToken = "idam-token";
        String serviceAuth = "service-auth";
        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();
        CaseResource caseResource = new CaseResource();
        caseResource.setReference("12345");

        when(authTokenGenerator.generate()).thenReturn(serviceAuth);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(idamToken);
        when(coreCaseDataApi.startEvent(idamToken, serviceAuth, "12345", EventId.markCaseForDeletion.name()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(eq(idamToken), eq(serviceAuth), eq("12345"), any(CaseDataContent.class)))
                .thenReturn(caseResource);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When
        CaseResource result = underTest.markCaseForDeletion(caseRef);

        // Then
        assertThat(result).isEqualTo(caseResource);
        verify(coreCaseDataApi).startEvent(idamToken, serviceAuth, "12345", EventId.markCaseForDeletion.name());
        verify(coreCaseDataApi).createEvent(eq(idamToken), eq(serviceAuth), eq("12345"),
                caseDataContentCaptor.capture());

        CaseDataContent capturedContent = caseDataContentCaptor.getValue();
        assertThat(capturedContent.getEvent().getId()).isEqualTo(EventId.markCaseForDeletion.name());
        assertThat(capturedContent.getEventToken()).isEqualTo("event-token");
    }

    @Test
    void shouldConfirmCaseDisposalWithCorrectEventId() throws Exception {
        // Given
        final long caseRef = 67890L;
        String idamToken = "idam-token";
        String serviceAuth = "service-auth";
        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();
        CaseResource caseResource = new CaseResource();
        caseResource.setReference("67890");

        when(authTokenGenerator.generate()).thenReturn(serviceAuth);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(idamToken);
        when(coreCaseDataApi.startEvent(idamToken, serviceAuth, "67890", EventId.confirmCaseDisposal.name()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(eq(idamToken), eq(serviceAuth), eq("67890"), any(CaseDataContent.class)))
                .thenReturn(caseResource);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When
        CaseResource result = underTest.confirmCaseDisposal(caseRef);

        // Then
        assertThat(result).isEqualTo(caseResource);
        verify(coreCaseDataApi).startEvent(idamToken, serviceAuth, "67890", EventId.confirmCaseDisposal.name());
        verify(coreCaseDataApi).createEvent(eq(idamToken), eq(serviceAuth), eq("67890"),
                caseDataContentCaptor.capture());

        CaseDataContent capturedContent = caseDataContentCaptor.getValue();
        assertThat(capturedContent.getEvent().getId()).isEqualTo(EventId.confirmCaseDisposal.name());
        assertThat(capturedContent.getEventToken()).isEqualTo("event-token");
    }

    @Test
    void shouldUseCaseReferenceAsStringInCoreDataApiCalls() throws Exception {
        // Given
        final long caseRef = 99999L;
        String idamToken = "idam-token";
        String serviceAuth = "service-auth";
        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();
        CaseResource caseResource = new CaseResource();
        caseResource.setReference("99999");

        when(authTokenGenerator.generate()).thenReturn(serviceAuth);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(idamToken);
        when(coreCaseDataApi.startEvent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(anyString(), anyString(), anyString(), any(CaseDataContent.class)))
                .thenReturn(caseResource);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When
        underTest.markCaseForDeletion(caseRef);

        // Then - verify case reference is converted to string
        verify(coreCaseDataApi).startEvent(idamToken, serviceAuth, "99999", EventId.markCaseForDeletion.name());
        verify(coreCaseDataApi).createEvent(eq(idamToken), eq(serviceAuth), eq("99999"), any(CaseDataContent.class));
    }

    @Test
    void shouldObtainAuthTokensForEachEvent() throws Exception {
        // Given
        final long caseRef = 11111L;
        String idamToken = "idam-token-1";
        String serviceAuth = "service-auth-1";
        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();
        CaseResource caseResource = new CaseResource();
        caseResource.setReference("11111");

        when(authTokenGenerator.generate()).thenReturn(serviceAuth);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(idamToken);
        when(coreCaseDataApi.startEvent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(anyString(), anyString(), anyString(), any(CaseDataContent.class)))
                .thenReturn(caseResource);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When
        underTest.markCaseForDeletion(caseRef);

        // Then
        verify(authTokenGenerator).generate();
        verify(systemUpdateUserTokenProvider).getAuthToken();
    }

    @Test
    void shouldConvertPcsCaseToJsonNodeForCaseDataContent() throws Exception {
        // Given
        final long caseRef = 22222L;
        String idamToken = "idam-token";
        String serviceAuth = "service-auth";
        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();
        CaseResource caseResource = new CaseResource();
        caseResource.setReference("22222");
        JsonNode expectedJsonNode = new ObjectMapper().readTree("{\"test\":\"data\"}");

        when(authTokenGenerator.generate()).thenReturn(serviceAuth);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(idamToken);
        when(coreCaseDataApi.startEvent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(anyString(), anyString(), anyString(), any(CaseDataContent.class)))
                .thenReturn(caseResource);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(expectedJsonNode);

        // When
        underTest.markCaseForDeletion(caseRef);

        // Then
        verify(coreCaseDataApi).createEvent(eq(idamToken), eq(serviceAuth), eq("22222"),
                caseDataContentCaptor.capture());
        CaseDataContent capturedContent = caseDataContentCaptor.getValue();
        assertThat(capturedContent.getData()).isEqualTo(expectedJsonNode);
    }
}
