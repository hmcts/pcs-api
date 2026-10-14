package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pcs.camunda.DmnValue;
import uk.gov.hmcts.reform.pcs.camunda.SendMessageRequest;
import uk.gov.hmcts.reform.pcs.camunda.WorkAllocationWorkflowApi;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})
@EnableFeignClients(clients = WorkAllocationWorkflowApi.class)
@TestPropertySource(properties = "work-allocation.workflow-api.url=http://localhost:8899")
@ExtendWith({PactConsumerTestExt.class, SpringExtension.class})
@PactTestFor(providerName = "wa_workflow_api_send_message", port = "8899")

public class WaWorkflowConsumerTest {

    private static final String SERVICE_AUTH_TOKEN = "Bearer serviceToken";

    @Autowired
    private WorkAllocationWorkflowApi workAllocationWorkflowApi;

    @Pact(provider = "wa_workflow_api_send_message", consumer = "pcs_api")
    public V4Pact executeSendMessage204(PactDslWithProvider builder) {

        return builder
            .given("send a message to Camunda")
            .uponReceiving("message to Post")
            .path("/workflow/message")
            .method("POST")
            .headers("ServiceAuthorization", SERVICE_AUTH_TOKEN,
                     "Content-Type", "application/json")
            .body(createMessage)
            .willRespondWith()
            .status(204)
            .status(HttpStatus.NO_CONTENT.value())
            .toPact(V4Pact.class);
    }

    PactDslJsonBody processVariables = (PactDslJsonBody) new PactDslJsonBody()
        .object("caseId")
        .stringType("value", "123412341234")
        .stringType("type", "String")
        .closeObject()
        .object("taskId")
        .stringType("value", "some taskId")
        .stringType("type", "String")
        .closeObject()
        .object("name")
        .stringType("value", "some name")
        .stringType("type", "String")
        .closeObject()
        .object("jurisdiction")
        .stringType("value", "PCS")
        .stringType("type", "String")
        .closeObject()
        .object("caseType")
        .stringType("value", "PCSCaseType")
        .stringType("type", "String")
        .closeObject();

    PactDslJsonBody createMessage = new PactDslJsonBody()
        .stringType("messageName", "some other message")
        .object("processVariables", processVariables)
        .nullValue("correlationKeys")
        .booleanType("all", false);

    Map<String, DmnValue<?>> message = Map.of(
        "caseId", new DmnValue<>("123412341234", "String"),
        "taskId", new DmnValue<>("some taskId", "String"),
        "name", new DmnValue<>("some name", "String"),
        "jurisdiction", new DmnValue<>("PCS", "String"),
        "caseType", new DmnValue<>("PCSCaseType", "String")
    );

    SendMessageRequest body = new SendMessageRequest(
        "some other message",
        message,
        null,
        false
    );

    @Test
    @PactTestFor(pactMethod = "executeSendMessage204")
    void sendMessage() {
        assertThatCode(() ->
                           workAllocationWorkflowApi.sendMessage(SERVICE_AUTH_TOKEN, body)
        ).doesNotThrowAnyException();
    }
}
