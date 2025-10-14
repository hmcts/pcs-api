package uk.gov.hmcts.reform.pcs.idam;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.exception.IdamException;
import uk.gov.hmcts.reform.pcs.idam.api.IdamTokenApi;
import uk.gov.hmcts.reform.pcs.idam.dto.IdamTokenResponse;

@Service
@Slf4j
public class PrdAdminTokenService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String GRANT_TYPE_PASSWORD = "password";

    private final IdamTokenApi idamTokenApi;
    private final String prdAdminUsername;
    private final String prdAdminPassword;
    private final String clientId;
    private final String clientSecret;
    private final String scope;

    public PrdAdminTokenService(
            IdamTokenApi idamTokenApi,
            @Value("${idam.prd-admin.username}") String prdAdminUsername,
            @Value("${idam.prd-admin.password}") String prdAdminPassword,
            @Value("${idam.client.id}") String clientId,
            @Value("${idam.client.secret}") String clientSecret,
            @Value("${idam.prd-admin.scope:openid profile roles}") String scope) {

        this.idamTokenApi = idamTokenApi;
        this.prdAdminUsername = prdAdminUsername;
        this.prdAdminPassword = prdAdminPassword;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }


    public String getPrdAdminToken() {
        try {
            IdamTokenResponse response = idamTokenApi.getToken(
                GRANT_TYPE_PASSWORD,
                prdAdminUsername,
                prdAdminPassword,
                clientId,
                clientSecret,
                scope
            );

            if (response != null && response.getAccessToken() != null) {
                return BEARER_PREFIX + response.getAccessToken();
            } else {
                log.error("Failed to retrieve PRD Admin token: Response or access token is null");
                throw new IdamException("Failed to retrieve PRD Admin token: Invalid response", null);
            }

        } catch (FeignException ex) {
            log.error("Feign error retrieving PRD Admin token. Status: {}, Message: {}",
                ex.status(), ex.getMessage(), ex);
            throw new IdamException("Unable to retrieve PRD Admin token for reference data access", ex);
        } catch (Exception ex) {
            log.error("Unexpected error retrieving PRD Admin token: {}", ex.getMessage(), ex);
            throw new IdamException("Unable to retrieve PRD Admin token for reference data access", ex);
        }
    }


}
