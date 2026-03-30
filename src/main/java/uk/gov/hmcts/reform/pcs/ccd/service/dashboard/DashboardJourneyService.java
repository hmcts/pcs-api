package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Computes dashboard notifications (template IDs + values) from submitted and draft case data.
 *
 * <p>READ-ONLY: callers are responsible for loading any data needed and passing it in.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardJourneyService {

    public List<DashboardNotification> computeNotifications(long caseReference,
                                                            State state,
                                                            PCSCase submittedCaseData,
                                                            Optional<PCSCase> draftCaseData) {

        boolean hasDraft = draftCaseData.isPresent();

        if (state == State.CASE_ISSUED) {
            DashboardNotification journeyNotification = DashboardNotification.builder()
                .templateId("Notice.PCS.Dashboard.CaseIssued")
                .templateValues(Map.of(
                    "caseReference", caseReference,
                    "hasDraft", hasDraft
                ))
                .build();

            return List.of(journeyNotification);
        }

        if (state == State.AWAITING_SUBMISSION_TO_HMCTS && hasDraft) {
            DashboardNotification resumeNotification = DashboardNotification.builder()
                .templateId("Notice.PCS.Dashboard.DraftInProgress")
                .templateValues(Map.of(
                    "caseReference", caseReference
                ))
                .build();
            return List.of(resumeNotification);
        }

        return List.of();
    }
}

