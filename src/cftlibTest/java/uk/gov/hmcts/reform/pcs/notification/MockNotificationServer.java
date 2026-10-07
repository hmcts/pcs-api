package uk.gov.hmcts.reform.pcs.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import wiremock.com.fasterxml.jackson.databind.node.IntNode;
import wiremock.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import wiremock.com.fasterxml.jackson.databind.node.ObjectNode;
import wiremock.com.fasterxml.jackson.databind.node.TextNode;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class MockNotificationServer {

    private static final String NOTIFICATION_URL = "/v2/notifications/email";

    private final ObjectMapper objectMapper;
    private final WireMockServer wireMockServer;

    public MockNotificationServer() {
        objectMapper = createObjectMapper();
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(9400));
    }

    public void startServer() {
        wireMockServer.start();
    }

    public void resetServer() {
        wireMockServer.resetAll();
        stubSendNotification();
    }

    public void stopServer() {
        wireMockServer.stop();
    }

    public List<CapturedNotification> getSentNotifications(int minNotificationCount) {
        waitForMinNotificationCount(minNotificationCount);

        return getLoggedRequests().stream()
            .map(this::toCapturedNotification)
            .toList();
    }

    private void waitForMinNotificationCount(int minNotificationCount) {
        await("At least %d notification(s) sent".formatted(minNotificationCount))
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .ignoreExceptions()
            .until(this::getSentNotificationCount, greaterThanOrEqualTo(minNotificationCount));
    }

    private int getSentNotificationCount() {
        List<LoggedRequest> loggedRequests = getLoggedRequests();
        return loggedRequests.size();
    }

    private List<LoggedRequest> getLoggedRequests() {
        return wireMockServer.findAll(RequestPatternBuilder.newRequestPattern().withUrl(NOTIFICATION_URL));
    }

    private CapturedNotification toCapturedNotification(LoggedRequest loggedRequest) {
        String jsonBody = loggedRequest.getBodyAsString();
        try {
            return objectMapper.readValue(jsonBody, CapturedNotification.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void stubSendNotification() {
        ObjectNode fakeNotifyResponse = createFakeNotifyResponse();

        wireMockServer
            .stubFor(WireMock.post(NOTIFICATION_URL).willReturn(WireMock.created().withJsonBody(fakeNotifyResponse)));
    }

    private static ObjectNode createFakeNotifyResponse() {
        ObjectNode contentNode = JsonNodeFactory.instance.objectNode();
        contentNode.set("body", TextNode.valueOf("some body"));
        contentNode.set("subject", TextNode.valueOf("some subject"));

        ObjectNode templateNode = JsonNodeFactory.instance.objectNode();
        templateNode.set("id", TextNode.valueOf(UUID.randomUUID().toString()));
        templateNode.set("version", IntNode.valueOf(10));
        templateNode.set("uri", TextNode.valueOf("some uri"));

        ObjectNode jsonBody = JsonNodeFactory.instance.objectNode();
        jsonBody.set("id", TextNode.valueOf(UUID.randomUUID().toString()));
        jsonBody.set("content", contentNode);
        jsonBody.set("template", templateNode);
        return jsonBody;
    }

    public ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .addModules(new ParameterNamesModule())
            .build();
    }

}
