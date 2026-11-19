package uk.gov.hmcts.reform.pcs.bulkclaim.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;

import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "bulk-claims")
@Validated
@Getter
@Setter
public class BulkClaimProperties {

    /** S2S service names of the APIM instances allowed to call /v2/bulk-claims/** (and nothing else). */
    @NotEmpty
    private List<@NotBlank String> apimServiceNames = List.of("pcs_bulk_claims_apim");

    /** Trusted header, set by APIM, carrying the PRD organisation id of the authenticated client. */
    @NotBlank
    private String organisationHeader = "X-Organisation-Id";

    @Min(1)
    private int maxClaimsPerRequest = 100;

    @NotNull
    private DataSize maxRequestSize = DataSize.ofMegabytes(10);

    @Min(1)
    private int parallelism = 4;

    @NotEmpty
    private List<ClaimantType> enabledClaimantTypes = List.of(
        ClaimantType.PROVIDER_OF_SOCIAL_HOUSING,
        ClaimantType.COMMUNITY_LANDLORD,
        ClaimantType.MORTGAGE_LENDER
    );

    @NotNull
    private Duration pendingRetryAfter = Duration.ofMinutes(10);

    @NotBlank
    private String systemReasonNoDocuments = "Not supplied via bulk interface";
}
