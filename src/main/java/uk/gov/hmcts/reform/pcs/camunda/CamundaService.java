package uk.gov.hmcts.reform.pcs.camunda;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

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

    private static final String CREATE = "createTaskMessage";

    public void createTask(Long caseId) {
        log.info("Creating task for {}", caseId);
        Map<String, DmnValue<?>> processVariables = new ConcurrentHashMap<>();

        LocalDateTime dueDate = LocalDateTime.now().plusDays(1);
        LocalDateTime delayUntil = LocalDateTime.now();

        // Required process variables
        // processVariables.put("idempotencyKey", dmnStringValue(idempotencyKey));
        processVariables.put("taskState", dmnStringValue("unconfigured"));
        processVariables.put("caseTypeId", dmnStringValue("pcs"));
        processVariables.put("dueDate", dmnStringValue(dueDate.format(ISO_LOCAL_DATE_TIME)));
        processVariables.put("workingDaysAllowed", dmnIntegerValue(1));
        processVariables.put("jurisdiction", dmnStringValue("pcs"));
        processVariables.put("name", dmnStringValue("Test Task"));
        processVariables.put("taskId", dmnStringValue("testTaskId"));
        processVariables.put("caseId", dmnStringValue(caseId.toString()));
        processVariables.put("delayUntil", dmnStringValue(delayUntil.format(ISO_LOCAL_DATE_TIME)));
        processVariables.put("hasWarnings", dmnBooleanValue(false));
        processVariables.put("warningList", dmnStringValue("[]"));

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
