package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Slf4j
@AllArgsConstructor
@Service
public class CamundaService {

    private final CamundaApi camundaApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final FeatureToggleService featureToggleService;

    private static final String CREATE = "createTaskMessage";
    private static final String UNCONFIGURED = "unconfigured";
    private static final String EMPTY_WARNINGS_LIST = "[]";
    private final Clock utcClock;

    public void createTask(
        Long caseId,
        TaskType taskType
    ) {
        if (!featureToggleService.isEnabled(FeatureFlag.CASEWORKER_WA)) {
            log.info("Skipped creating task for {}", caseId);
            return;
        }

        log.info("Creating task for {}", caseId);
        Map<String, DmnValue<?>> processVariables = new ConcurrentHashMap<>();

        LocalDateTime delayUntil = LocalDateTime.now(utcClock);
        LocalDateTime dueDate = delayUntil.plusDays(taskType.getWorkingDays());

        processVariables.put("taskState", dmnStringValue(UNCONFIGURED));
        processVariables.put("caseTypeId", dmnStringValue(CaseType.getCaseType()));
        processVariables.put("dueDate", dmnStringValue(dueDate.format(ISO_LOCAL_DATE_TIME)));
        processVariables.put("workingDaysAllowed", dmnIntegerValue(taskType.getWorkingDays()));
        processVariables.put("jurisdiction", dmnStringValue(CaseType.getJurisdictionId()));
        processVariables.put("name", dmnStringValue(taskType.getName()));
        processVariables.put("taskId", dmnStringValue(taskType.getId()));
        processVariables.put("caseId", dmnStringValue(caseId.toString()));
        processVariables.put("delayUntil", dmnStringValue(delayUntil.format(ISO_LOCAL_DATE_TIME)));
        processVariables.put("hasWarnings", dmnBooleanValue(false));
        processVariables.put("warningList", dmnStringValue(EMPTY_WARNINGS_LIST));

        SendMessageRequest request = SendMessageRequest.builder()
            .messageName(CREATE)
            .processVariables(processVariables)
            .build();

        String s2sToken = authTokenGenerator.generate();

        try {
            log.info("Camunda request for case id {}: {}", caseId, request);
            camundaApi.sendMessage(s2sToken, request);
        } catch (Exception e) {
            log.error("Failed to send Camunda request for caseId {}", caseId, e);
        }

    }

    private DmnValue<String> dmnStringValue(String value) {
        return DmnValue.<String>builder().value(value).type("String").build();
    }

    private DmnValue<Integer> dmnIntegerValue(Integer value) {
        return DmnValue.<Integer>builder().value(value).type("Integer").build();
    }

    private DmnValue<Boolean> dmnBooleanValue(Boolean value) {
        return DmnValue.<Boolean>builder().value(value).type("Boolean").build();
    }
}
