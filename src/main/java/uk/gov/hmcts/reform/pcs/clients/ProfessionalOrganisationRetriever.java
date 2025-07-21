package uk.gov.hmcts.reform.pcs.clients;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.dto.OrganisationDto;

@Service
public class ProfessionalOrganisationRetriever {
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private final AuthTokenGenerator serviceAuthTokenGenerator;

    public ProfessionalOrganisationRetriever(AuthTokenGenerator serviceAuthTokenGenerator){
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
    }

    public void retrieve(){
        //get tokens for request
        final String serviceAuthorizationToken = serviceAuthTokenGenerator.generate().split(" ")[1];
        //using hardcoded user token for poc.

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, "X");
        headers.set(SERVICE_AUTHORIZATION, "X");

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<OrganisationDto> responseString =
            new RestTemplate().exchange("http://rd-professional-api-aat.service.core-compute-aat.internal/refdata/external/v2/organisations",
                                        HttpMethod.GET,
                                        requestEntity,
                                        OrganisationDto.class);

        System.out.println(responseString.toString());
    }
}
