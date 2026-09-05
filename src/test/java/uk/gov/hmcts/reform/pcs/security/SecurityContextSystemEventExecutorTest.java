package uk.gov.hmcts.reform.pcs.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.ccd.sdk.ActorAttribution;
import uk.gov.hmcts.ccd.sdk.SystemEventAction;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutionResult;
import uk.gov.hmcts.ccd.sdk.SystemEventExecutor;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityContextSystemEventExecutorTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final UUID IDEMPOTENCY_KEY = UUID.randomUUID();
    private static final SystemEventAction ACTION = context -> null;

    @Mock
    private SystemEventExecutor delegate;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private IdamAuthenticator idamAuthenticator;

    private SecurityContextSystemEventExecutor underTest;

    @BeforeEach
    void setUp() {
        underTest = new SecurityContextSystemEventExecutor(
            delegate, systemUpdateUserTokenProvider, idamAuthenticator);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("Bearer system");
        User systemUser = new User("Bearer system",
            UserInfo.builder().uid("system-uid").givenName("Service").familyName("Account").build());
        when(idamAuthenticator.validateAuthToken("Bearer system")).thenReturn(systemUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void delegatesWithTheResolvedSystemUserInTheSecurityContext() {
        SystemEventExecutionResult expected =
            new SystemEventExecutionResult(1L, SystemEventExecutionResult.Outcome.EXECUTED);
        when(delegate.execute(anyLong(), any(UUID.class), any())).thenAnswer(invocation -> {
            User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.getAuthToken()).isEqualTo("Bearer system");
            assertThat(principal.getUserDetails().getUid()).isEqualTo("system-uid");
            return expected;
        });

        SystemEventExecutionResult result = underTest.execute(CASE_REFERENCE, IDEMPOTENCY_KEY, ACTION);

        assertThat(result).isSameAs(expected);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void delegatesTheActorOverloadWithTheSameContext() {
        ActorAttribution actor = new ActorAttribution("actor-id", "Jane", "Doe");
        SystemEventExecutionResult expected =
            new SystemEventExecutionResult(2L, SystemEventExecutionResult.Outcome.REPLAYED);
        when(delegate.execute(anyLong(), any(ActorAttribution.class), any(UUID.class), any()))
            .thenAnswer(invocation -> {
                assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
                return expected;
            });

        SystemEventExecutionResult result = underTest.execute(CASE_REFERENCE, actor, IDEMPOTENCY_KEY, ACTION);

        assertThat(result).isSameAs(expected);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void restoresTheSecurityContextWhenTheDelegateThrows() {
        doThrow(new IllegalStateException("boom"))
            .when(delegate).execute(anyLong(), any(UUID.class), any());

        assertThatThrownBy(() -> underTest.execute(CASE_REFERENCE, IDEMPOTENCY_KEY, ACTION))
            .isInstanceOf(IllegalStateException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
