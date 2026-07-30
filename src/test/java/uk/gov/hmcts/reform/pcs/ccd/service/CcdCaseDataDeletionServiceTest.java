package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.model.DraftCasesToDiscard;
import uk.gov.hmcts.reform.pcs.ccd.repository.CcdCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.casedeletion.CcdCaseDataDeletionService;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcdCaseDataDeletionServiceTest {

    private final int discardAfterDays = 30;
    private final long caseRef = 123456789L;
    private final long caseRef2 = 987654321L;
    private final String caseReference = "123456789";


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
        when(ccdCaseRepository.findExpiredDraftCases(discardAfterDays)).thenReturn(List.of());
        // When
        List<DraftCasesToDiscard> result = underTest.findExpiredDraftCases(discardAfterDays);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnValidListWhenExpiredCasesFound() {
        // Given
        when(ccdCaseRepository.findExpiredDraftCases(discardAfterDays))
                .thenReturn(List.of(
                        DraftCasesToDiscard.builder().caseReference(caseRef).build(),
                        DraftCasesToDiscard.builder().caseReference(caseRef2).build()
                ));
        // When
        List<DraftCasesToDiscard> result = underTest.findExpiredDraftCases(discardAfterDays);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldInvokeDeleteCcdCaseData() {
        // Given & When
        underTest.deleteCcdCaseData(caseRef);

        // Then
        verify(ccdCaseRepository).deleteCcdCaseData(caseRef);
    }

    @Test
    void shouldMarkCaseForDeletionWithCorrectEventId() throws Exception {
        // Given
        String idamToken = "idam-token";
        String serviceAuth = "service-auth";
        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();
        CaseResource caseResource = new CaseResource();
        caseResource.setReference(caseReference);

        when(authTokenGenerator.generate()).thenReturn(serviceAuth);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(idamToken);
        when(coreCaseDataApi.startEvent(idamToken, serviceAuth, caseReference, EventId.markCaseForDeletion.name()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(eq(idamToken), eq(serviceAuth), eq(caseReference), any(CaseDataContent.class)))
                .thenReturn(caseResource);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When
        CaseResource result = underTest.markCaseForDeletion(caseRef);

        // Then
        assertThat(result).isEqualTo(caseResource);
        verify(coreCaseDataApi).startEvent(idamToken, serviceAuth, caseReference, EventId.markCaseForDeletion.name());
        verify(coreCaseDataApi).createEvent(eq(idamToken), eq(serviceAuth), eq(caseReference),
                caseDataContentCaptor.capture());

        CaseDataContent capturedContent = caseDataContentCaptor.getValue();
        assertThat(capturedContent.getEvent().getId()).isEqualTo(EventId.markCaseForDeletion.name());
        assertThat(capturedContent.getEventToken()).isEqualTo("event-token");
    }

    @Test
    void shouldConfirmCaseDisposalWithCorrectEventId() throws Exception {
        // Given
        String idamToken = "idam-token";
        String serviceAuth = "service-auth";
        final StartEventResponse startEventResponse = StartEventResponse.builder().token("event-token").build();
        CaseResource caseResource = new CaseResource();
        caseResource.setReference(caseReference);

        when(authTokenGenerator.generate()).thenReturn(serviceAuth);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(idamToken);
        when(coreCaseDataApi.startEvent(idamToken, serviceAuth, caseReference, EventId.confirmCaseDisposal.name()))
                .thenReturn(startEventResponse);
        when(coreCaseDataApi.createEvent(eq(idamToken), eq(serviceAuth), eq(caseReference), any(CaseDataContent.class)))
                .thenReturn(caseResource);
        when(objectMapper.valueToTree(any(PCSCase.class))).thenReturn(new ObjectMapper().readTree("{}"));

        // When
        CaseResource result = underTest.confirmCaseDisposal(caseRef);

        // Then
        assertThat(result).isEqualTo(caseResource);
        verify(coreCaseDataApi).startEvent(idamToken, serviceAuth, caseReference, EventId.confirmCaseDisposal.name());
        verify(coreCaseDataApi).createEvent(eq(idamToken), eq(serviceAuth), eq(caseReference),
                caseDataContentCaptor.capture());

        CaseDataContent capturedContent = caseDataContentCaptor.getValue();
        assertThat(capturedContent.getEvent().getId()).isEqualTo(EventId.confirmCaseDisposal.name());
        assertThat(capturedContent.getEventToken()).isEqualTo("event-token");
    }
}
