package uk.gov.hmcts.reform.pcs.reference.service;

import static java.util.Objects.nonNull;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

@Service
@Slf4j
public class OrganisationDetailsService {

    private final RdProfessionalApi rdProfessionalApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final IdamTokenProvider prdAdminTokenProvider;

    public OrganisationDetailsService(
            RdProfessionalApi rdProfessionalApi,
            AuthTokenGenerator authTokenGenerator,
            @Qualifier("prdAdminTokenProvider") IdamTokenProvider prdAdminTokenProvider) {
        this.rdProfessionalApi = rdProfessionalApi;
        this.authTokenGenerator = authTokenGenerator;
        this.prdAdminTokenProvider = prdAdminTokenProvider;
    }

    /** Retrieves organisation details for a given user ID. TODO move to OrganisationService */
    @Deprecated
    public OrganisationDetailsResponse getOrganisationDetails(String userId) {
        try {
            return fetchOrganisationDetails(userId);
        } catch (OrganisationDetailsException ex) {
            return null;
        }
    }

    /** The same lookup, but a failure is raised rather than reported as "no organisation".
     * TODO move to OrganisationService */
    @Deprecated
    public OrganisationDetailsResponse requireOrganisationDetails(String userId) {
        OrganisationDetailsResponse details = fetchOrganisationDetails(userId);
        if (nonNull(details)) {
            return details;
        }
        return null;
    }

    /** TODO move to OrganisationService. */
    @Deprecated
    private OrganisationDetailsResponse fetchOrganisationDetails(String userId) {
        try {
            String s2sToken = authTokenGenerator.generate();
            String prdAdminToken = prdAdminTokenProvider.getAuthToken();

            OrganisationDetailsResponse details = rdProfessionalApi.getOrganisationDetails(
                userId, s2sToken, prdAdminToken
            );

            if (details == null) {
                log.warn("Organisation details response is null");
            }

            return details;

        } catch (FeignException.NotFound ex) {
            // Normal for citizens (no organisation), so not logged as an error.
            log.debug("No organisation held in rd-professional");
            return null;
        } catch (FeignException ex) {
            log.error("Feign error retrieving organisation details. Status: {}", ex.status(), ex);
            throw new OrganisationDetailsException("Failed to retrieve organisation details", ex);
        } catch (Exception ex) {
            log.error("Unexpected error retrieving organisation details", ex);
            throw new OrganisationDetailsException("Unexpected error retrieving organisation details", ex);
        }
    }

}
