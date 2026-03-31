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

        log.info("DashboardJourneyService.computeNotifications called for caseReference={}, state={}, hasDraft={}",
                 caseReference, state, hasDraft);

        if (state == State.CASE_ISSUED) {
            DashboardNotification journeyNotification = DashboardNotification.builder()
                .templateId("Notice.PCS.Dashboard.CaseIssued")
                .templateValues(Map.of(
                    "caseReference", caseReference,
                    "hasDraft", hasDraft
                ))
                .build();

            log.info("DashboardJourneyService selected templateId={} for caseReference={}, state={}",
                     journeyNotification.getTemplateId(), caseReference, state);
            return List.of(journeyNotification);
        }

        if (state == State.AWAITING_SUBMISSION_TO_HMCTS && hasDraft) {
            DashboardNotification resumeNotification = DashboardNotification.builder()
                .templateId("Notice.PCS.Dashboard.DraftInProgress")
                .templateValues(Map.of(
                    "caseReference", caseReference
                ))
                .build();
            log.info("DashboardJourneyService selected templateId={} for caseReference={}, state={}",
                     resumeNotification.getTemplateId(), caseReference, state);
            return List.of(resumeNotification);
        }

        log.info(
            "DashboardJourneyService no matching dashboard notification rules for caseReference={}, state={}",
            caseReference, state);
        return List.of();
    }
}

