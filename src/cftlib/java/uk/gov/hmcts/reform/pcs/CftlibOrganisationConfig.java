package uk.gov.hmcts.reform.pcs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

/**
 * Stubs organisation lookups for local and CFTLib test runs.
 *
 * <p>PRD (rd-professional) is not bundled in CFTLib, so any call through
 * {@link OrganisationService} would fail with a connection error. This {@code @Primary}
 * override returns a fixed test organisation, bypassing PRD entirely, so that case creation
 * (which requires an org ID for group-access derivation) succeeds. The org ID must match
 * the {@code caseAccessGroupId} configured in {@code cftlib-am-role-assignments.json}.</p>
 */
@Configuration
public class CftlibOrganisationConfig {

    /**
     * Must match the {@code caseAccessGroupId} suffix in cftlib-am-role-assignments.json.
     */
    static final String TEST_ORG_ID = "TEST-123";

    @Bean("cftlibOrganisationService")
    @Primary
    public OrganisationService organisationService() {
        return new OrganisationService(null, null) {
            @Override
            public String getOrganisationIdForCurrentUser() {
                return TEST_ORG_ID;
            }

            @Override
            public String getOrgProfileIdForCurrentUser() {
                return OrganisationProfile.SOLICITOR_PROFILE.getId();
            }

            @Override
            public String getOrganisationNameForCurrentUser() {
                return "Test Organisation";
            }

            @Override
            public uk.gov.hmcts.ccd.sdk.type.AddressUK getOrganisationAddressForCurrentUser() {
                return null;
            }
        };
    }
}
