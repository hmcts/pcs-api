package uk.gov.hmcts.reform.pcs.bulkclaim.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.unit.DataSize;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BulkClaimPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(Config.class);

    @Test
    void shouldApplyDefaults() {
        runner.run(context -> {
            BulkClaimProperties properties = context.getBean(BulkClaimProperties.class);

            assertThat(properties.getApimServiceNames()).containsExactly("pcs_bulk_claims_apim");
            assertThat(properties.getOrganisationHeader()).isEqualTo("X-Organisation-Id");
            assertThat(properties.getMaxClaimsPerRequest()).isEqualTo(100);
            assertThat(properties.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(10));
            assertThat(properties.getParallelism()).isEqualTo(4);
            assertThat(properties.getEnabledClaimantTypes()).containsExactly(
                ClaimantType.PROVIDER_OF_SOCIAL_HOUSING, ClaimantType.COMMUNITY_LANDLORD,
                ClaimantType.MORTGAGE_LENDER);
            assertThat(properties.getPendingRetryAfter()).isEqualTo(Duration.ofMinutes(10));
            assertThat(properties.getSystemReasonNoDocuments()).isEqualTo("Not supplied via bulk interface");
        });
    }

    @Test
    void shouldBindOverrides() {
        runner.withPropertyValues(
            "bulk-claims.apim-service-names=a,b",
            "bulk-claims.organisation-header=X-Org",
            "bulk-claims.max-claims-per-request=5",
            "bulk-claims.max-request-size=2MB",
            "bulk-claims.parallelism=2",
            "bulk-claims.enabled-claimant-types=MORTGAGE_LENDER",
            "bulk-claims.pending-retry-after=30s"
        ).run(context -> {
            BulkClaimProperties properties = context.getBean(BulkClaimProperties.class);

            assertThat(properties.getApimServiceNames()).containsExactly("a", "b");
            assertThat(properties.getOrganisationHeader()).isEqualTo("X-Org");
            assertThat(properties.getMaxClaimsPerRequest()).isEqualTo(5);
            assertThat(properties.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(2));
            assertThat(properties.getParallelism()).isEqualTo(2);
            assertThat(properties.getEnabledClaimantTypes()).containsExactly(ClaimantType.MORTGAGE_LENDER);
            assertThat(properties.getPendingRetryAfter()).isEqualTo(Duration.ofSeconds(30));
        });
    }

    @Test
    void shouldRejectInvalidValues() {
        runner.withPropertyValues("bulk-claims.parallelism=0")
            .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(BulkClaimProperties.class)
    static class Config {
    }
}
