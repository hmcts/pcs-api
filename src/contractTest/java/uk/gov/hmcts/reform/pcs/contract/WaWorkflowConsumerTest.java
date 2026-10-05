package uk.gov.hmcts.reform.pcs.contract;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.Request;
import au.com.dius.pact.core.model.Response;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Map;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.pcs.camunda.DmnValue;
import uk.gov.hmcts.reform.pcs.camunda.SendMessageRequest;

@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    FeignClientsConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})
@EnableFeignClients(clients = WorkAllocationWorkflowApi.class) //is this right
@TestPropertySource(properties = "wa_workflow.api.url=http://localhost:8899") //any number in here atm - what local port is?
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "wa_workflow_api_send_message", port = "8899") //tbd

@RequiredArgsConstructor

public class WaWorkflowConsumerTest {

    private static final String SERVICE_AUTH_TOKEN = "Bearer serviceToken";

    @Autowired
    private WorkAllocationWorkflowApi workAllocationWorkflowApi; //not needed? Accessing right?
    private ObjectMapper objectMapper = new ObjectMapper();

    @Pact(provider = "wa_workflow_api_send_message", consumer = "pcs_api")
    public V4Pact executeSendMessage204(PactDslWithProvider builder) throws JsonProcessingException {

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

    PactDslJsonBody createMessage = new PactDslJsonBody() //- try build like this - can always return as a string but it may not like it
        .stringType("caseId", "some CaseId")
        .stringType("taskId", "SomeTaskId")
        .stringType("name", "some Name")
        .stringType("jurisdiction", "PCS")
        .stringType("caseType", "PCSCaseType");

    String createMessageBody = createMessage.getBody().toString();
    SendMessageRequest body = new SendMessageRequest(
        "one",
        (Map<String, DmnValue<?>>) createMessage,
        null,
        false);



    @Test
    @PactTestFor(pactMethod = "executeSendMessage204")
    void sendMessage() {
        workAllocationWorkflowApi.sendMessage(SERVICE_AUTH_TOKEN, body).getStatus();
        assertThat(messageResponse.getStatus()).isEqualTo(204);
        assertThat(messageResponse.getBody()).isNull();
    }

}

   /*
   public interface WorkAllocationWorkflowApi {
    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @PostMapping(
        value = "/workflow/message",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    void sendMessage(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                     @RequestBody SendMessageRequest sendMessageRequest);

}




   public String createMessage() throws JsonProcessingException {

        Map<String, DmnValue<?>> processVariables = new HashMap<>(); //concurrentHashMap

        processVariables.put("caseId", new DmnValue<> ("some caseId", "String")); //dmstringvalyr(id.tostring) if cALLIBG EXSTING CASE ID ETC
        processVariables.put("taskId", new DmnValue<>("some taskId", "String"));
        processVariables.put("name", new DmnValue<>("some name", "String"));
        processVariables.put("jurisdiction", new DmnValue<>("PCS", "String"));
        processVariables.put("caseType", new DmnValue<>("PcSCaseType", "String"));

    @Test
    @PactTestFor(pactMethod = "executeSendMessage204")
    void sendMessage() throws JsonProcessingException {
        Response messageResponse = workAllocationWorkflowApi.sendMessage(SERVICE_AUTH_TOKEN, request);
        assertThat(messageResponse.getStatus()).isEqualTo(204);
        assertThat(messageResponse.getBody()).isNull();
    }
    */
        /*
        *   PactDslJsonBody responseBody = new PactDslJsonBody() - try build like this - can always return as a string but it may not like it
            .stringType("code", "FEE0412")
            .stringType("description", "Recovery of Land - County Court")
            .integerType("version", 4)
            .decimalType("fee_amount", 404.00);

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
            .messageName("some other message")
            .processVariables(processVariables)
            .all(false)
            .build();

        return objectMapper.writeValueAsString(sendMessageRequest); //???? build as a dsl object
    }*/
