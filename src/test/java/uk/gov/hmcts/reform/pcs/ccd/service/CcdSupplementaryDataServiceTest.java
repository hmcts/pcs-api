package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcdSupplementaryDataServiceTest {

    @Mock
    private IdamService idamService;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private CoreCaseDataApi coreCaseDataApi;

    private CcdSupplementaryDataService underTest;

    @Captor
    private ArgumentCaptor<Map<String, Map<String, Map<String, Object>>>> requestCaptor;

    private static final String CASE_ID = "1234567890123456";
    private static final String USER_TOKEN = "Bearer userToken";
    private static final String S2S_TOKEN = "Bearer serviceToken";
    private static final String HMCTS_SERVICE_ID = "AAA3";

    @BeforeEach
    void setUp() {
        underTest = new CcdSupplementaryDataService(idamService, authTokenGenerator, coreCaseDataApi, HMCTS_SERVICE_ID);
    }

    @Test
    void shouldCallSubmitSupplementaryDataWithCorrectTokens() {
        // Given
        when(idamService.getSystemUserAuthorisation()).thenReturn(USER_TOKEN);
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);

        // When
        underTest.submitSupplementaryDataRequestToCcd(CASE_ID);

        // Then
        verify(coreCaseDataApi).submitSupplementaryData(
            eq(USER_TOKEN), eq(S2S_TOKEN), eq(CASE_ID), requestCaptor.capture()
        );
    }

    @Test
    void shouldSubmitCorrectSupplementaryDataRequestStructure() {
        // Given
        when(idamService.getSystemUserAuthorisation()).thenReturn(USER_TOKEN);
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);

        // When
        underTest.submitSupplementaryDataRequestToCcd(CASE_ID);

        // Then
        verify(coreCaseDataApi).submitSupplementaryData(
            eq(USER_TOKEN), eq(S2S_TOKEN), eq(CASE_ID), requestCaptor.capture()
        );

        Map<String, Map<String, Map<String, Object>>> request = requestCaptor.getValue();
        assertThat(request).containsKey("supplementary_data_updates");

        Map<String, Map<String, Object>> updates = request.get("supplementary_data_updates");
        assertThat(updates).containsKey("$set");

        Map<String, Object> setOperation = updates.get("$set");
        assertThat(setOperation).containsEntry("HMCTSServiceId", HMCTS_SERVICE_ID);
    }
}
