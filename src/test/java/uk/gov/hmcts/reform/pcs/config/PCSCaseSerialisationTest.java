package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;

import static org.assertj.core.api.Assertions.assertThat;

class PCSCaseSerialisationTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().getMapper();

    @Test
    void shouldNotEchoLinkedCasesComponentLauncherInCallbackResponses() throws Exception {
        PCSCase pcsCase = objectMapper.readValue(
            """
                {
                  "LinkedCasesComponentLauncher": {},
                  "featureFlags": {
                    "release1dot2Enabled": "YES"
                  }
                }
                """,
            PCSCase.class
        );

        assertThat(pcsCase.getLinkedCasesComponentLauncher()).isNotNull();
        assertThat(pcsCase.getFeatureFlags().getRelease1dot2Enabled().name()).isEqualTo("YES");
        assertThat(objectMapper.writeValueAsString(pcsCase))
            .doesNotContain("LinkedCasesComponentLauncher");
    }

    @Test
    void shouldPreserveUnwrappedTenancyCcdFieldIds() throws Exception {
        PCSCase pcsCase = PCSCase.builder()
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                .typeOfTenancyLicence(TenancyLicenceType.SECURE_TENANCY)
                .build())
            .build();

        assertThat(objectMapper.writeValueAsString(pcsCase))
            .contains("\"tenancy_TypeOfTenancyLicence\":\"SECURE_TENANCY\"")
            .doesNotContain("tenancy_typeOfTenancyLicence");
    }
}
