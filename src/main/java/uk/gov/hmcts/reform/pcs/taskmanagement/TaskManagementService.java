package uk.gov.hmcts.reform.pcs.taskmanagement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.taskmanagement.TaskOutboxService;
import uk.gov.hmcts.ccd.sdk.taskmanagement.model.TaskPayload;
import uk.gov.hmcts.ccd.sdk.taskmanagement.model.TaskPermission;
import uk.gov.hmcts.ccd.sdk.taskmanagement.model.outbox.TerminateTaskOutboxPayload;
import uk.gov.hmcts.ccd.sdk.taskmanagement.model.request.TaskCreateRequest;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.taskmanagement.model.TaskType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.COMMA_DELIMITER;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskConstants.DEFAULT_EXECUTION_TYPE;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskConstants.DEFAULT_LOCATION;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskConstants.DEFAULT_REGION;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskConstants.DEFAULT_SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskConstants.DEFAULT_TASK_SYSTEM;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskConstants.MAJOR_PRIORITY;
import static uk.gov.hmcts.reform.pcs.taskmanagement.TaskConstants.MINOR_PRIORITY;

@Service
@RequiredArgsConstructor
public class TaskManagementService {

    private final AddressFormatter addressFormatter;
    private final TaskOutboxService taskOutboxService;

    public void enqueueInitiationTasks(List<TaskType> taskTypes, PCSCase caseData, long caseId) {
        if (taskTypes == null || taskTypes.isEmpty()) {
            return;
        }

        taskTypes.stream()
            .distinct()
            .map(taskType -> new TaskCreateRequest(getTaskPayload(taskType, caseData, caseId)))
            .forEach(taskOutboxService::enqueueTaskCreateRequest);
    }

    public void enqueueCompletionTasks(List<TaskType> taskTypes, long caseId) {
        enqueueTaskTermination(taskTypes, caseId, true);
    }

    public void enqueueCancellationTasks(List<TaskType> taskTypes, long caseId) {
        enqueueTaskTermination(taskTypes, caseId, false);
    }

    private void enqueueTaskTermination(List<TaskType> taskTypes, long caseId, boolean completion) {
        if (taskTypes == null || taskTypes.isEmpty()) {
            return;
        }

        TerminateTaskOutboxPayload taskOutboxPayload = new TerminateTaskOutboxPayload(
            String.valueOf(caseId),
            CaseType.getCaseType(),
            taskTypes.stream().map(Enum::name).toList()
        );

        if (completion) {
            taskOutboxService.enqueueTaskCompleteRequest(taskOutboxPayload);
        } else {
            taskOutboxService.enqueueTaskCancelRequest(taskOutboxPayload);
        }
    }

    private TaskPayload getTaskPayload(TaskType taskType, PCSCase caseData, long caseReference) {
        return TaskPayload.builder()
            .externalTaskId(UUID.randomUUID().toString())
            .name(taskType.getName())
            .type(taskType.name())
            .title(taskType.getName())
            .created(OffsetDateTime.now())
            .executionType(DEFAULT_EXECUTION_TYPE)
            .taskSystem(DEFAULT_TASK_SYSTEM)
            .caseId(String.valueOf(caseReference))
            .jurisdiction(CaseType.getJurisdictionId())
            .caseTypeId(CaseType.getCaseType())
            .securityClassification(DEFAULT_SECURITY_CLASSIFICATION)
            .permissions(getTaskPermissions())
            .priorityDate(OffsetDateTime.now().plusDays(3))
            .caseName(getCaseName(caseData))
            .caseCategory("Possession Claim")
            .region(DEFAULT_REGION)
            .location(DEFAULT_LOCATION)
            .locationName("Central London County Court")
            .majorPriority(MAJOR_PRIORITY)
            .minorPriority(MINOR_PRIORITY)
            .dueDateTime(OffsetDateTime.now().plusDays(5))
            .workType(WorkType.DECISION_MAKING_WORK.getLowerCaseName())
            .roleCategory(RoleCategory.ADMIN.name())
            .description("Example task description")
            .build();
    }

    private String getCaseName(PCSCase caseData) {
        return addressFormatter.formatShortAddress(caseData.getPropertyAddress(), COMMA_DELIMITER);
    }

    private List<TaskPermission> getTaskPermissions() {
        return Stream.of(TaskAccess.PCS_ADMIN).map(TaskAccess::toTaskPermission).toList();
    }

}
