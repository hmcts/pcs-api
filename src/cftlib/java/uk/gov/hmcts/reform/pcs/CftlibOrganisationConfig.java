package uk.gov.hmcts.reform.pcs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

import java.util.List;

/** CFTLib has no PRD; return the test org that matches cftlib-am-role-assignments.json. */
@Configuration
public class CftlibOrganisationConfig {

    /** Must match the caseAccessGroupId suffix in cftlib-am-role-assignments.json. */
    static final String TEST_ORG_ID = "TEST-123";

    @Bean("cftlibOrganisationService")
    @Primary
    public OrganisationService organisationService() {
        return new OrganisationService(null, null, null) {
            @Override
            public OrganisationDetailsResponse getOrganisationDetailsForCurrentUser() {
                OrganisationDetailsResponse response = new OrganisationDetailsResponse();
                response.setOrganisationIdentifier(TEST_ORG_ID);
                response.setOrganisationProfileIds(List.of(OrganisationProfile.SOLICITOR_PROFILE.getId()));
                response.setName("Test Organisation");
                response.setContactInformation(List.of());
                return response;
            }

            @Override
            public String getOrganisationName(OrganisationDetailsResponse orgDetails) {
                return "Test Organisation";
            }

            @Override
            public uk.gov.hmcts.ccd.sdk.type.AddressUK getOrganisationAddress(OrganisationDetailsResponse orgDetails) {
                return null;
            }

            @Override
            public String getOrganisationIdForCurrentUser() {
                return TEST_ORG_ID;
            }

            @Override
            public String requireOrganisationIdForCurrentUser() {
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
