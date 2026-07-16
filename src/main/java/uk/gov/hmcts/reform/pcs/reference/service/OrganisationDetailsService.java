package uk.gov.hmcts.reform.pcs.reference.service;

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

    /**
     * Retrieves organisation details for a given user ID.
     * @param userId The user ID to get organisation details for
     * @return OrganisationDetailsResponse containing organisation information
     */
    public OrganisationDetailsResponse getOrganisationDetails(String userId) {
        try {
            String s2sToken = authTokenGenerator.generate();
            String prdAdminToken = prdAdminTokenProvider.getAuthToken();

            OrganisationDetailsResponse details = rdProfessionalApi.getOrganisationDetails(
                userId, s2sToken, prdAdminToken
            );

            if (details == null) {
                log.warn("Organisation details response is null for userId: {}", userId);
            }

            return details;

        } catch (FeignException ex) {
            log.error("Feign error retrieving organisation details for userId: {}. Status: {}, Message: {}",
                userId, ex.status(), ex.getMessage(), ex);
            throw new OrganisationDetailsException("Failed to retrieve organisation details", ex);
        } catch (Exception ex) {
            log.error("Unexpected error retrieving organisation details for userId: {}. Error: {}",
                userId, ex.getMessage(), ex);
            throw new OrganisationDetailsException("Unexpected error retrieving organisation details", ex);
        }
    }
}
