package uk.gov.hmcts.reform.pcs.noc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.SystemEventRecordingService;
import uk.gov.hmcts.ccd.sdk.SystemEventRecordingService.ActorAttribution;
import uk.gov.hmcts.reform.pcs.ccd.model.NocAccessChangeTaskData;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

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

    public NoticeOfChangeAppliedEventService(
        SystemEventRecordingService systemEventRecordingService,
        @Qualifier("systemUpdateUserTokenProvider") IdamTokenProvider systemUpdateUserTokenProvider) {
        this.systemEventRecordingService = systemEventRecordingService;
        this.systemUpdateUserTokenProvider = systemUpdateUserTokenProvider;
    }

    public void submit(long caseReference, NocAccessChangeTaskData taskData) {
        log.debug("Recording {} for case {}", noticeOfChangeApplied, caseReference);
        systemEventRecordingService.recordSystemEvent(
            caseReference,
            noticeOfChangeApplied.name(),
            systemUpdateUserTokenProvider.getAuthToken(),
            isNotBlank(taskData.getEmail()) ? "Notice of change by " + taskData.getEmail() : null,
            new ActorAttribution(taskData.getUserId(), taskData.getFirstName(), taskData.getLastName())
        );
    }
}
