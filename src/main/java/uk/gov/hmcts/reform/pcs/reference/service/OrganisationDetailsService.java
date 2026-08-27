package uk.gov.hmcts.reform.pcs.reference.service;

import static java.util.Objects.nonNull;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
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

    /** Organisation name for a user (claimant name population). */
    public String getOrganisationName(String userId) {
        OrganisationDetailsResponse details = getOrganisationDetails(userId);
        if (nonNull(details)) {
            return details.getName();
        }
        return null;
    }

    /** Organisation address for a user (claimant address population), or null if none. */
    public AddressUK getOrganisationAddress(String userId) {

        OrganisationDetailsResponse organisationDetails = getOrganisationDetails(userId);

        return getOrganisationAddress(organisationDetails);
    }

    /** Organisation address from a details response, or null if none. */
    public AddressUK getOrganisationAddress(OrganisationDetailsResponse organisationDetails) {
        if (organisationDetails == null || organisationDetails.getContactInformation().isEmpty()) {
            return null;
        }

        OrganisationDetailsResponse.ContactInformation contactInfo = organisationDetails
            .getContactInformation().getFirst();

        return AddressUK.builder()
            .addressLine1(contactInfo.getAddressLine1())
            .addressLine2(contactInfo.getAddressLine2())
            .addressLine3(contactInfo.getAddressLine3())
            .postTown(contactInfo.getTownCity())
            .county(contactInfo.getCounty())
            .country(contactInfo.getCountry())
            .postCode(contactInfo.getPostCode())
            .build();
    }

    /** Organisation identifier for a user. */
    public String getOrganisationIdentifier(String userId) {
        OrganisationDetailsResponse details = getOrganisationDetails(userId);
        if (nonNull(details)) {
            return details.getOrganisationIdentifier();
        }
        return null;
    }

    /** Retrieves organisation details for a given user ID. */
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

        } catch (FeignException.NotFound ex) {
            log.error("No organisation held in rd-professional for userId: {}", userId);
            return null;
        } catch (FeignException ex) {
            log.error("Feign error retrieving organisation details for userId: {}. Status: {}, Message: {}",
                      userId, ex.status(), ex.getMessage(), ex);
            return null;
        } catch (Exception ex) {
            log.error("Unexpected error retrieving organisation details for userId: {}. Error: {}",
                      userId, ex.getMessage(), ex);
            return null;
        }
    }
}
