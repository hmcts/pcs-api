package uk.gov.hmcts.reform.pcs.ccd.service.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardData;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.DashboardNotification;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.ResponseStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TemplateValue;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task.TaskGroupEvaluator;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Computes dashboard data (notifications + task groups) from submitted case data.
 * READ-ONLY
 */
@Service
@Slf4j
public class DashboardJourneyService {

    private static final List<TaskGroupId> TASK_GROUP_ORDER = List.of(
        TaskGroupId.CLAIM,
        TaskGroupId.DOCUMENTS,
        TaskGroupId.RESPONSE,
        TaskGroupId.HEARING,
        TaskGroupId.NOTICE,
        TaskGroupId.APPLICATIONS
    );

    private final List<TaskGroupEvaluator> evaluatorsInOrder;
    private final DraftCaseDataService draftCaseDataService;
    private final DefendantResponseService defendantResponseService;

    public DashboardJourneyService(
        DraftCaseDataService draftCaseDataService,
        DefendantResponseService defendantResponseService,
        List<TaskGroupEvaluator> evaluators
    ) {
        this.draftCaseDataService = draftCaseDataService;
        this.defendantResponseService = defendantResponseService;
        this.evaluatorsInOrder = evaluators.stream()
            .sorted(Comparator.comparingInt(e -> orderIndex(e.groupId())))
            .toList();
    }

    public DashboardData computeDashboardData(long caseReference, PCSCase submittedCaseData) {
        return computeDashboardData(caseReference, submittedCaseData, null, null);
    }

    public DashboardData computeDashboardData(
        long caseReference,
        PCSCase submittedCaseData,
        PcsCaseEntity caseEntity,
        PartyEntity defendant
    ) {

        boolean hasDraftResponse = draftCaseDataService.hasMeaningfulRespondDraft(
            caseReference, EventId.respondPossessionClaim);
        boolean hasSubmittedResponse = defendantResponseService.hasSubmittedResponse(caseReference);

        DashboardContext ctx = new DashboardContext(
            caseReference,
            caseEntity,
            defendant,
            hasDraftResponse,
            hasSubmittedResponse
        );

        ResponseStatus responseStatus = getResponseStatus(hasDraftResponse, hasSubmittedResponse);

        List<ListValue<DashboardNotification>> notifications = computeNotifications(responseStatus);
        List<ListValue<TaskGroup>> taskGroups = computeTaskGroups(ctx);

        log.info("DashboardJourneyService computed {} notification(s) and {} taskGroup(s) for case={}",
                 notifications.size(), taskGroups.size(), caseReference);

        return DashboardData.builder()
            .caseId(String.valueOf(caseReference))
            .propertyAddress(submittedCaseData.getPropertyAddress())
            .notifications(notifications)
            .taskGroups(taskGroups)
            .build();
    }

    private List<ListValue<DashboardNotification>> computeNotifications(ResponseStatus responseStatus) {
        String responseTemplateId = switch (responseStatus) {
            case NOT_STARTED -> "Defendant.ResponseNotStarted";
            case IN_PROGRESS -> "Defendant.ResponseInProgress";
            case SUBMITTED -> "Defendant.ResponseSubmitted";
        };

        return ListValueUtils.wrapListItems(List.of(
            DashboardNotification.builder()
                .templateId("Defendant.NoHearingArranged")
                .templateValues(toTemplateValues(Map.of()))
                .build(),
            DashboardNotification.builder()
                .templateId(responseTemplateId)
                .templateValues(toTemplateValues(Map.of()))
                .build()
        ));
    }

    private List<ListValue<TaskGroup>> computeTaskGroups(DashboardContext ctx) {
        List<TaskGroup> groups = evaluatorsInOrder.stream()
            .map(e -> e.evaluate(ctx))
            .toList();
        return ListValueUtils.wrapListItems(groups);
    }

    private ResponseStatus getResponseStatus(boolean hasDraft, boolean hasSubmitted) {
        if (hasSubmitted) {
            return ResponseStatus.SUBMITTED;
        }
        return hasDraft ? ResponseStatus.IN_PROGRESS : ResponseStatus.NOT_STARTED;
    }

    /**
     * Entries with a null value are omitted so optional placeholders (for example {@code ctaLink}) can be left out
     * of the map rather than sent as null.
     */
    private List<ListValue<TemplateValue>> toTemplateValues(Map<String, String> values) {
        return ListValueUtils.wrapListItems(
            values.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> TemplateValue.builder()
                    .key(e.getKey())
                    .value(e.getValue())
                    .build())
                .toList()
        );
    }

    private static int orderIndex(TaskGroupId id) {
        int idx = TASK_GROUP_ORDER.indexOf(id);
        return idx >= 0 ? idx : Integer.MAX_VALUE;
    }
}
