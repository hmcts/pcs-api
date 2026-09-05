package uk.gov.hmcts.reform.pcs.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.ActorAttribution;
import uk.gov.hmcts.ccd.sdk.SystemEventAction;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Wraps the SDK executor and runs it as the authenticated system user: the case projection inside
 * the event transaction reads the current user and their auth token from the security context,
 * which the background threads that record system events do not have. The system user is resolved
 * from its own token the same way every request resolves its user, so no user id is configured.
 */
@Component
@Primary
public class SecurityContextSystemEventExecutor implements SystemEventExecutor {

    private final SystemEventExecutor delegate;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final IdamAuthenticator idamAuthenticator;

    public SecurityContextSystemEventExecutor(
        @Qualifier("systemEventExecutorImpl") SystemEventExecutor delegate,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        IdamAuthenticator idamAuthenticator
    ) {
        this.delegate = delegate;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.idamAuthenticator = idamAuthenticator;
    }

    @Override
    public SystemEventExecutionResult execute(long caseReference, UUID idempotencyKey, SystemEventAction action) {
        return runAsSystemUser(() -> delegate.execute(caseReference, idempotencyKey, action));
    }

    @Override
    public SystemEventExecutionResult execute(long caseReference, ActorAttribution actor, UUID idempotencyKey,
                                              SystemEventAction action) {
        return runAsSystemUser(() -> delegate.execute(caseReference, actor, idempotencyKey, action));
    }

    private SystemEventExecutionResult runAsSystemUser(Supplier<SystemEventExecutionResult> execution) {
        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            User systemUser = idamAuthenticator.validateAuthToken(systemUpdateUserTokenProvider.getAuthToken());
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(systemUser, null, Collections.emptyList()));
            return execution.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }
    }
}
