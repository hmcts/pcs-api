package uk.gov.hmcts.reform.pcs.reference.service;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.exception.OrganisationDetailsException;
import uk.gov.hmcts.reform.pcs.idam.PrdAdminTokenService;
import uk.gov.hmcts.reform.pcs.reference.api.RdProfessionalApi;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.util.List;

@Service
@Slf4j
public class OrganisationDetailsService {

    private final RdProfessionalApi rdProfessionalApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final PrdAdminTokenService prdAdminTokenService;

    @Value("${reference-data.dev-organisation-fallback-enabled:false}")
    private boolean devOrganisationFallbackEnabled;

    public OrganisationDetailsService(
            RdProfessionalApi rdProfessionalApi,
            AuthTokenGenerator authTokenGenerator,
            PrdAdminTokenService prdAdminTokenService) {
        this.rdProfessionalApi = rdProfessionalApi;
        this.authTokenGenerator = authTokenGenerator;
        this.prdAdminTokenService = prdAdminTokenService;
    }

    /**
     * Retrieves organisation details for a given user ID.
     * @param userId The user ID to get organisation details for
     * @return OrganisationDetailsResponse containing organisation information
     */
    public OrganisationDetailsResponse getOrganisationDetails(String userId) {
        try {
            String s2sToken = authTokenGenerator.generate();
            String prdAdminToken = prdAdminTokenService.getPrdAdminToken();

            OrganisationDetailsResponse details = rdProfessionalApi.getOrganisationDetails(
                userId, s2sToken, prdAdminToken
            );

            if (details == null) {
                log.warn("Organisation details response is null for userId: {}", userId);
            }

            return details;

        } catch (FeignException ex) {
            if (devOrganisationFallbackEnabled) {
                log.warn("Using dev organisation details fallback for userId: {}", userId);
                return devOrganisationDetails();
            }
            log.error("Feign error retrieving organisation details for userId: {}. Status: {}, Message: {}",
                userId, ex.status(), ex.getMessage(), ex);
            throw new OrganisationDetailsException("Failed to retrieve organisation details", ex);
        } catch (Exception ex) {
            if (devOrganisationFallbackEnabled) {
                log.warn("Using dev organisation details fallback for userId: {}", userId);
                return devOrganisationDetails();
            }
            log.error("Unexpected error retrieving organisation details for userId: {}. Error: {}",
                userId, ex.getMessage(), ex);
            throw new OrganisationDetailsException("Unexpected error retrieving organisation details", ex);
        }
    }

    /**
     * Gets the organisation name for a given user ID (for claimant name population).
     * @param userId The user ID to get organisation name for
     * @return Organisation name
     */
    public String getOrganisationName(String userId) {
        OrganisationDetailsResponse details = getOrganisationDetails(userId);
        return details.getName();
    }

    /**
     * Gets the organisation address for a given user ID (for claimant address population).
     * @param userId The user ID to get organisation address for
     * @return Organisation address or null if no address information is available
     */
    public AddressUK getOrganisationAddress(String userId) {

        OrganisationDetailsResponse organisationDetails = getOrganisationDetails(userId);

        return getOrganisationAddress(organisationDetails);
    }

    /**
     * Gets the organisation address extracted from a given organisation details response.
     * @param organisationDetails The organisation details response get organisation address from
     * @return Organisation address or null if no address information is available
     */
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

    /**
     * Gets the organisation identifier for a given user ID.
     * @param userId The user ID to get organisation identifier for
     * @return Organisation identifier
     */
    public String getOrganisationIdentifier(String userId) {
        OrganisationDetailsResponse details = getOrganisationDetails(userId);
        return details.getOrganisationIdentifier();
    }

    private OrganisationDetailsResponse devOrganisationDetails() {
        return OrganisationDetailsResponse.builder()
            .name("PCS Local Solicitor Organisation")
            .organisationIdentifier("PCSLOCAL")
            .status("ACTIVE")
            .contactInformation(List.of(OrganisationDetailsResponse.ContactInformation.builder()
                .addressLine1("1 Local Solicitor Street")
                .townCity("London")
                .country("United Kingdom")
                .postCode("SW1A 1AA")
                .build()))
            .build();
    }
}
