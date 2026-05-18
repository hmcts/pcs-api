package uk.gov.hmcts.reform.pcs.ccd.event.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseResource;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.payment;

@ExtendWith(MockitoExtension.class)
class CcdUpdateServiceTest {

    private static final String CASE_ID = "1111-2222-3333-4444";
    private static final String IDAM_TOKEN = "Bearer idam";
    private static final String S2S_TOKEN = "Bearer s2s";
    private static final String EVENT_TOKEN = "event-token";

    @Mock
    private IdamService idamService;
    @Mock
    private AuthTokenGenerator s2sAuthTokenGenerator;
    @Mock
    private CoreCaseDataApi coreCaseDataApi;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CcdUpdateService underTest;

    @Test
    void shouldStartEventAndSubmitPaymentSuccessfully() {
        // Given
        when(idamService.getSystemUserAuthorisation()).thenReturn(IDAM_TOKEN);
        when(s2sAuthTokenGenerator.generate()).thenReturn(S2S_TOKEN);

        StartEventResponse startEventResponse = StartEventResponse.builder().token(EVENT_TOKEN).build();
        when(coreCaseDataApi.startEvent(IDAM_TOKEN, S2S_TOKEN, CASE_ID, payment.name()))
            .thenReturn(startEventResponse);

        CaseResource expectedCaseResource = new CaseResource();
        when(coreCaseDataApi.createEvent(eq(IDAM_TOKEN), eq(S2S_TOKEN), eq(CASE_ID),
                                         org.mockito.ArgumentMatchers.any(CaseDataContent.class)))
            .thenReturn(expectedCaseResource);
        when(objectMapper.valueToTree(any())).thenReturn(mock(JsonNode.class));

        // When
        CaseResource result = underTest.submitPaymentSuccess(CASE_ID);

        // Then
        assertThat(result).isSameAs(expectedCaseResource);
        verify(coreCaseDataApi).startEvent(IDAM_TOKEN, S2S_TOKEN, CASE_ID, payment.name());
        ArgumentCaptor<CaseDataContent> contentCaptor = ArgumentCaptor.forClass(CaseDataContent.class);
        verify(coreCaseDataApi).createEvent(eq(IDAM_TOKEN), eq(S2S_TOKEN), eq(CASE_ID), contentCaptor.capture());
        CaseDataContent submitted = contentCaptor.getValue();
        assertThat(submitted.getEventToken()).isEqualTo(EVENT_TOKEN);
        assertThat(submitted.getEvent().getId()).isEqualTo(payment.name());
    }
}
