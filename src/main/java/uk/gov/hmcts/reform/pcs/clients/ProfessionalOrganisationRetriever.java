package uk.gov.hmcts.reform.pcs.clients;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.dto.OrganisationDto;

import java.util.Random;

/**
 * Retriever class to handle pulling the user's organisation data and returning it
 * as a POJO
 */
@Service
public class ProfessionalOrganisationRetriever {
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    private final AuthTokenGenerator serviceAuthTokenGenerator;

    private final String userToken = "";
    private final String serviceToken = "";
    private Random random;

    public ProfessionalOrganisationRetriever(AuthTokenGenerator serviceAuthTokenGenerator){
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator; //generating a token with this seemed to fail. Switching to hardcode
        serviceAuthTokenGenerator.generate();
        random = new Random();
    }

    public String retrieve(){

//        ----- Approach 1 -----
//        ----- This approach will not work ----
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
//        headers.set(HttpHeaders.AUTHORIZATION, userToken);
//        headers.set(SERVICE_AUTHORIZATION, serviceToken);
//
//        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
//
//        return new RestTemplate().exchange("http://rd-professional-api-aat.service.core-compute-aat.internal/refdata/external/v2/organisations",
//                                       HttpMethod.GET,
//                                       requestEntity,
//                                       OrganisationDto.class);
//


//        ----- Approach 2 -----
        //get ID of user
//        String userId = getUserId(userToken);
//
//       String adminEmail = ""; //pull from env variables and add role to yaml.
//
//        //get Admins user token
//        String adminUserToken = getAdminToken(adminEmail);
//
//        //get org details with user ID, authorised by prd-admin user.
//        return getOrgDetails(userId, adminUserToken);
        return null;
    }

    private String getUserId(String userToken){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, userToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        return new RestTemplate().exchange("https://idam-api.aat.platform.hmcts.net/o/userinfo?claims=" + userToken,
                                           HttpMethod.GET,
                                           requestEntity,
                                           String.class).getBody();
    }

    private String getAdminToken(String adminEmail){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"password\": \"Pa$$w0rd\",\n" +
            "    \"username\":\"" + adminEmail + "\"{\n" +
            "        \"email\":\"" + adminEmail + "\",\n" +
            "        \"forename\":\"x\",\n" +
            "        \"surname\":\"x\",\n" +
            "        \"roleNames\": [\n" +
            "            \"caseworker\"\n" +
            "        ]\n" +
            "    }}";

        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

        return new RestTemplate().exchange("https://idam-api.aat.platform.hmcts.net/o/userinfo?claims=" + userToken,
                                           HttpMethod.GET,
                                           requestEntity,
                                           String.class).getBody();
    }

    private String getOrgDetails(String userId, String adminToken){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, userToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        return new RestTemplate().exchange("https://idam-api.aat.platform.hmcts.net/o/userinfo?claims=" + userToken,
                                           HttpMethod.GET,
                                           requestEntity,
                                           String.class).getBody();
    }
}
