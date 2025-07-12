package uk.gov.hmcts.reform.pcs.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdamAuthenticationFilterTest {

    @Mock
    private IdamService idamService;
    @Mock
    private HttpServletRequest request;

    private IdamAuthenticationFilter underTest;

    @BeforeEach
    void setUp() {
        underTest = new IdamAuthenticationFilter(idamService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/courts", "/courts/1", "/ccd", "/ccd/cases"})
    void shouldFilterForSpecificRequestPaths(String requestURI) {
        when(request.getRequestURI()).thenReturn(requestURI);

        boolean shouldNotFilter = underTest.shouldNotFilter(request);

        assertThat(shouldNotFilter).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/court/1", "/welcome", "/"})
    void shouldNotFilterForOtherRequestPaths(String requestURI) {
        when(request.getRequestURI()).thenReturn(requestURI);

        boolean shouldNotFilter = underTest.shouldNotFilter(request);

        assertThat(shouldNotFilter).isTrue();
    }

}
