package uk.gov.hmcts.reform.pcs.clients;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.dto.OrganisationDto;
import uk.gov.hmcts.reform.pcs.idam.IdamService;

/**
 * Retriever class to handle pulling the user's organisation data and returning it
 * as a POJO.
 */
@Service
public class ProfessionalOrganisationRetriever {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private final AuthTokenGenerator serviceAuthTokenGenerator;
    private final IdamService idamService;

    public ProfessionalOrganisationRetriever(AuthTokenGenerator serviceAuthTokenGenerator, IdamService idamService) {
        //generating a token with this seemed to fail. Switching to hardcode
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        serviceAuthTokenGenerator.generate();
        this.idamService = idamService;
    }

    public OrganisationDto retrieve(String uid) {
        //gets bearer token for system user. We need this to authorise our request for organisation details
        final String systemUserAuth = idamService.getSystemUserAuthorisation();
        //We now pass in our userid of our currently logged-in user, and the auth token from the system user
        return getOrgDetails(uid, systemUserAuth);
    }

    private OrganisationDto getOrgDetails(String userId, String adminToken) {
        String serviceAuthorizationToken = serviceAuthTokenGenerator.generate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, adminToken);
        headers.set(SERVICE_AUTHORIZATION, serviceAuthorizationToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        return new RestTemplate().exchange("http://rd-professional-api-aat.service.core-compute-aat.internal/refdata/internal/v1/organisations/orgDetails/" + userId,
            HttpMethod.GET,
            requestEntity,
            OrganisationDto.class).getBody();
    }
}
