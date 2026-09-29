package uk.gov.hmcts.reform.pcs.security;


import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty("idam.s2s-authorised.services")
public class ServiceAuthTokenValidator implements AuthTokenValidator {

    private final ServiceAuthorisationApi api;

    @Autowired
    public ServiceAuthTokenValidator(ServiceAuthorisationApi api) {
        this.api = api;
    }

    @Override
    public void validate(final String token) {
        validate(token, Collections.emptyList());
    }

    @Override
    public void validate(String token, final List<String> roles) {
        try {
            api.authorise(token, roles.toArray(new String[0]));
        } catch (FeignException exception) {
            throw parseFeignException(exception);
        }
    }

    @Override
    public String getServiceName(final String token) {
        try {
            return api.getServiceName(token);
        } catch (FeignException exception) {
            throw parseFeignException(exception);
        }
    }

    private RuntimeException parseFeignException(FeignException exception) {
        boolean isClientError = exception.status() >= 400 && exception.status() <= 499;

        if (isClientError) {
            return new InvalidTokenException(exception.getMessage(), exception);
        } else {
            return new ServiceException(exception.getMessage(), exception);
        }
    }
}
