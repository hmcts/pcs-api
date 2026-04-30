package uk.gov.hmcts.reform.pcs.ccd.service.dashboard.task;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.Task;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroup;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskGroupId;
import uk.gov.hmcts.reform.pcs.ccd.domain.dashboard.TaskStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.SectionStatusEntry;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.dashboard.DashboardContext;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@Component
public class ResponseTaskGroupEvaluator implements TaskGroupEvaluator {

    private static final String TASK_CHECK_YOUR_ANSWERS_AND_SUBMIT = "Defendant.RTC.CheckYourAnswersAndSubmit";

    private final DraftCaseDataService draftCaseDataService;
    private final DefendantResponseRepository defendantResponseRepository;
    private final SecurityContextService securityContextService;

    public ResponseTaskGroupEvaluator(DraftCaseDataService draftCaseDataService,
                                      DefendantResponseRepository defendantResponseRepository,
                                      SecurityContextService securityContextService) {
        this.draftCaseDataService = draftCaseDataService;
        this.defendantResponseRepository = defendantResponseRepository;
        this.securityContextService = securityContextService;
    }

    @Override
    public TaskGroupId groupId() {
        return TaskGroupId.RESPONSE;
    }

    @Override
    public TaskGroup evaluate(DashboardContext ctx) {
        long caseReference = ctx.caseReference();
        boolean responseSubmitted = hasSubmittedDefendantResponse(caseReference);
        boolean paymentsApplicable = isPaymentsSectionApplicable(ctx.submittedCaseData());
        Map<RespondToClaimSection, SectionStatus> sectionStatuses = getSectionStatuses(caseReference);

        List<Task> sectionTasks = RespondToClaimSection.applicableInOrder(paymentsApplicable)
            .stream()
            .map(section -> sectionTask(section, sectionStatuses, responseSubmitted))
            .collect(Collectors.toCollection(ArrayList::new));

        boolean allSectionsDone = allSectionsCompleted(sectionStatuses, paymentsApplicable);
        TaskStatus checkYourAnswersStatus = allSectionsDone || responseSubmitted
            ? TaskStatus.AVAILABLE
            : TaskStatus.NOT_AVAILABLE;

        sectionTasks.add(Task.builder()
            .templateId(TASK_CHECK_YOUR_ANSWERS_AND_SUBMIT)
            .status(checkYourAnswersStatus)
            .build());

        return TaskGroup.builder()
            .groupId(TaskGroupId.RESPONSE)
            .tasks(ListValueUtils.wrapListItems(sectionTasks))
            .build();
    }

    private Task sectionTask(RespondToClaimSection section,
                             Map<RespondToClaimSection, SectionStatus> sectionStatuses,
                             boolean responseSubmitted) {
        TaskStatus status = responseSubmitted
            ? TaskStatus.COMPLETED
            : toTaskStatus(sectionStatuses.get(section));
        return Task.builder()
            .templateId(section.templateId())
            .status(status)
            .build();
    }

    private boolean allSectionsCompleted(
        Map<RespondToClaimSection, SectionStatus> sectionStatuses,
        boolean paymentsApplicable
    ) {
        return RespondToClaimSection.applicableInOrder(paymentsApplicable)
            .stream()
            .allMatch(section -> sectionStatuses.get(section) == SectionStatus.COMPLETED);
    }

    private TaskStatus toTaskStatus(SectionStatus status) {
        if (status == SectionStatus.COMPLETED) {
            return TaskStatus.COMPLETED;
        }
        if (status == SectionStatus.IN_PROGRESS) {
            return TaskStatus.IN_PROGRESS;
        }
        return TaskStatus.AVAILABLE;
    }

    private Map<RespondToClaimSection, SectionStatus> getSectionStatuses(long caseReference) {
        return draftCaseDataService.getUnsubmittedCaseData(caseReference, respondPossessionClaim)
            .map(PCSCase::getPossessionClaimResponse)
            .map(PossessionClaimResponse::getDefendantResponses)
            .map(DefendantResponses::getSectionStatuses)
            .map(this::toSectionStatusMap)
            .orElse(Map.of());
    }

    private Map<RespondToClaimSection, SectionStatus> toSectionStatusMap(List<ListValue<SectionStatusEntry>> list) {
        if (list == null || list.isEmpty()) {
            return Map.of();
        }
        Map<RespondToClaimSection, SectionStatus> map = new EnumMap<>(RespondToClaimSection.class);
        for (ListValue<SectionStatusEntry> listValue : list) {
            if (listValue == null || listValue.getValue() == null) {
                continue;
            }
            SectionStatusEntry entry = listValue.getValue();
            Optional<RespondToClaimSection> section = RespondToClaimSection.fromSectionId(entry.getSectionId());
            Optional<SectionStatus> status = SectionStatus.from(entry.getStatus());
            if (section.isPresent() && status.isPresent()) {
                map.put(section.get(), status.get());
            }
        }
        return map;
    }

    private boolean isPaymentsSectionApplicable(PCSCase submittedCaseData) {
        if (submittedCaseData == null) {
            return false;
        }
        return Optional.ofNullable(submittedCaseData.getClaimGroundSummaries())
            .orElse(List.of())
            .stream()
            .map(ListValue::getValue)
            .anyMatch(this::isRentArrearsGround);
    }

    private boolean isRentArrearsGround(ClaimGroundSummary ground) {
        return ground != null && ground.getIsRentArrears() == YesOrNo.YES;
    }

    private boolean hasSubmittedDefendantResponse(long caseReference) {
        UUID currentUser = securityContextService.getCurrentUserId();
        if (currentUser == null) {
            return false;
        }

        return defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            caseReference,
            currentUser
        );
    }
}
