package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private CcdSupplementaryDataService underTest;

    private static final long CASE_REFERENCE = 1234L;

    @Test
    void shouldCallSubmitSupplementaryData() {
        String userAuth = "Bearer userToken";
        String serviceAuth = "Bearer serviceToken";

        when(idamService.getSystemUserAuthorisation()).thenReturn(userAuth);
        when(authTokenGenerator.generate()).thenReturn(serviceAuth);

        underTest.submitSupplementaryDataToCcd(String.valueOf(CASE_REFERENCE));

        verify(coreCaseDataApi).submitSupplementaryData(eq(userAuth),
            eq(serviceAuth), eq(String.valueOf(CASE_REFERENCE)), any());
    }
}
