package uk.gov.hmcts.reform.pcs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.idam.User;

import java.util.UUID;

@Service
public class SecurityContextService {

    private final String systemUserId;

    public SecurityContextService(
        @Value("${ccd.decentralised-runtime.system-user.id}") String systemUserId) {
        this.systemUserId = systemUserId;
    }

    /**
     * True when the current principal is the configured system-event identity. That identity
     * exists in no external service, so user-scoped lookups must short-circuit rather than
     * query IDAM, rd-professional or case assignment for it.
     */
    public boolean isSystemUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
            && authentication.getPrincipal() instanceof User user
            && systemUserId.equals(user.getUserDetails().getUid());
    }

    /**
     * Gets the current user ID from the {@link SecurityContext}.
     * @return The user ID for the user making the current request
     * @throws SecurityContextException if the security principal is not set or is not a {@link User} type
     */
    public UUID getCurrentUserId() {
        UserInfo userDetails = getCurrentUserDetails();
        return userDetails != null ? UUID.fromString(userDetails.getUid()) : null;
    }

    public String getCurrentUserAuthToken() {
        return getCurrentUser().getAuthToken();
    }

    /**
     * Gets the current user details from the {@link SecurityContext}.
     * @return The user details for the user making the current request
     * @throws SecurityContextException if the security principal is not set or is not a {@link User} type
     */
    public UserInfo getCurrentUserDetails() {
        return getCurrentUser().getUserDetails();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new SecurityContextException("No authentication instance found");
        }

        if (authentication.getPrincipal() instanceof User user) {
            return user;
        } else {
            throw new SecurityContextException("Authentication principal is null or not of the expected type");
        }
    }

}
