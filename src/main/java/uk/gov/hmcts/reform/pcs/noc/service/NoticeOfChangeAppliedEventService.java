package uk.gov.hmcts.reform.pcs.noc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.SystemEventRecordingService;
import uk.gov.hmcts.ccd.sdk.SystemEventRecordingService.ActorAttribution;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.idam.IdamAuthenticator;
import uk.gov.hmcts.reform.pcs.idam.User;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.noticeOfChangeApplied;

/**
 * Records the {@code noticeOfChangeApplied} system event once a Notice of Change has been applied.
 * The event stores a fresh case snapshot, which is what carries the re-derived CaseAccessGroups into
 * the search index, and it appears in the case history attributed to the acting solicitor.
 */
@Service
@Slf4j
public class NoticeOfChangeAppliedEventService {

    private final SystemEventRecordingService systemEventRecordingService;
    private final IdamTokenProvider systemUpdateUserTokenProvider;
    private final IdamAuthenticator idamAuthenticator;

    public NoticeOfChangeAppliedEventService(
        SystemEventRecordingService systemEventRecordingService,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider,
        IdamAuthenticator idamAuthenticator) {
        this.systemEventRecordingService = systemEventRecordingService;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
        this.idamAuthenticator = idamAuthenticator;
    }

    public void submit(long caseReference, NocAccessChangeTaskData taskData) {
        log.debug("Recording {} for case {}", noticeOfChangeApplied, caseReference);
        String systemToken = systemUpdateUserTokenProvider.getAuthToken();
        runAsSystemUser(systemToken, () -> recordEvent(caseReference, taskData, systemToken));
    }

    private void runAsSystemUser(String systemToken, Runnable action) {
        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            User systemUser = idamAuthenticator.validateAuthToken(systemToken);
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(systemUser, null, Collections.emptyList()));
            action.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }
    }

    private void recordEvent(long caseReference, NocAccessChangeTaskData taskData, String systemToken) {
        systemEventRecordingService.recordSystemEvent(
            caseReference,
            noticeOfChangeApplied.name(),
            systemToken,
            actingSolicitorSummary(taskData),
            actingSolicitor(taskData),
            retrySafeIdempotencyKey(taskData)
        );
    }

    private static String actingSolicitorSummary(NocAccessChangeTaskData taskData) {
        return isNotBlank(taskData.getEmail()) ? "Notice of change by " + taskData.getEmail() : null;
    }

    private static ActorAttribution actingSolicitor(NocAccessChangeTaskData taskData) {
        return new ActorAttribution(taskData.getUserId(), taskData.getFirstName(), taskData.getLastName());
    }

    private static UUID retrySafeIdempotencyKey(NocAccessChangeTaskData taskData) {
        return Objects.requireNonNullElseGet(taskData.getEventIdempotencyKey(), UUID::randomUUID);
    }
}
