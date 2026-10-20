package uk.gov.hmcts.reform.pcs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.exception.SecurityContextException;
import uk.gov.hmcts.reform.pcs.idam.IdamUserIds;
import uk.gov.hmcts.reform.pcs.idam.User;


@Service
public class SecurityContextService {

    private final String systemUserId;
    private final String idamSystemUsername;

    public SecurityContextService(
        @Value("${ccd.decentralised-runtime.system-user.id}") String systemUserId,
        @Value("${idam.system-user.username}") String idamSystemUsername) {
        this.systemUserId = systemUserId;
        this.idamSystemUsername = idamSystemUsername;
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
     * True when the current principal is PCS's IDAM system account (idam.system-user.username), which background
     * tasks such as bulk print use to call other services. Unlike {@link #isSystemUser()} it is a real IDAM user.
     */
    public boolean isIdamSystemUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
            && authentication.getPrincipal() instanceof User user
            && idamSystemUsername.equalsIgnoreCase(user.getUserDetails().getSub());
    }

    /**
     * Gets the current user ID from the {@link SecurityContext}.
     * @return The user ID for the user making the current request
     * @throws SecurityContextException if the security principal is not set or is not a {@link User} type
     */
    public String getCurrentUserId() {
        UserInfo userDetails = getCurrentUserDetails();
        return userDetails != null ? toUserId(userDetails.getUid()) : null;
    }

    public String toUserId(String uid) {
        return IdamUserIds.normalise(uid);
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
