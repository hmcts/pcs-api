package uk.gov.hmcts.reform.pcs.testingsupport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CcdTestCaseOrchestratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CcdTestCaseOrchestrator underTest = new CcdTestCaseOrchestrator(
        null,
        null,
        objectMapper
    );

    @Test
    void shouldPreserveCcdFieldIdCasingAcrossTheJacksonClientBoundary() throws Exception {
        Map<String, Object> caseData = underTest.toCaseData(
            objectMapper.readTree("""
                {
                  "tenancy_TypeOfTenancyLicence": "SECURE_TENANCY"
                }
                """)
        );

        assertThat(caseData)
            .containsEntry("tenancy_TypeOfTenancyLicence", "SECURE_TENANCY")
            .doesNotContainKey("tenancy_typeOfTenancyLicence");
    }
}
