package uk.gov.hmcts.reform.pcs.ccd.page.createtestcase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MakeAClaimTest {

    @Mock
    private EligibilityService eligibilityService;

    @InjectMocks
    private MakeAClaim makeAClaim;

    @Test
    void shouldImplementCcdPageConfiguration() {
        // Then
        assertTrue(makeAClaim instanceof CcdPageConfiguration);
    }

    @Test
    void shouldInstantiateWithoutErrors() {
        // When & Then
        assertDoesNotThrow(() -> new MakeAClaim(eligibilityService));
    }

    @Test
    void shouldHaveEligibilityServiceInjected() {
        // Then
        assertNotNull(makeAClaim);
        assertNotNull(eligibilityService);
    }
} 