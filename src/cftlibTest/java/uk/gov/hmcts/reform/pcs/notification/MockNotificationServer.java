package uk.gov.hmcts.reform.pcs.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import wiremock.com.fasterxml.jackson.databind.node.IntNode;
import wiremock.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import wiremock.com.fasterxml.jackson.databind.node.ObjectNode;
import wiremock.com.fasterxml.jackson.databind.node.TextNode;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@Component
@RequiredArgsConstructor
public class MockNotificationServer {

    private static final String NOTIFICATION_URL = "/v2/notifications/email";

    private final WireMockServer wireMockServer;
    private final ObjectMapper objectMapper;
    private final CaseReferenceFormatter caseReferenceFormatter;

    @PostConstruct
    public void stubEndpoints() {
        stubSendNotification();
    }

    public List<CapturedNotification> getSentNotifications(long caseReference,
                                                           int minNotificationCount,
                                                           Instant sentSince) {

        waitForMinNotificationCount(caseReference, minNotificationCount, sentSince);

        return getLoggedRequests(caseReference, sentSince).stream()
            .map(this::toCapturedNotification)
            .toList();
    }

    private void waitForMinNotificationCount(long caseReference, int minNotificationCount, Instant sentSince) {
        await("At least %d notification(s) sent".formatted(minNotificationCount))
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .ignoreExceptions()
            .until(
                () -> getSentNotificationCount(caseReference, sentSince), greaterThanOrEqualTo(minNotificationCount)
            );
    }

    private int getSentNotificationCount(long caseReference, Instant sentSince) {
        List<LoggedRequest> loggedRequests = getLoggedRequests(caseReference, sentSince);
        return loggedRequests.size();
    }

    private List<LoggedRequest> getLoggedRequests(long caseReference, Instant sentSince) {
        String caseReferenceWithDashes = caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference);

        List<LoggedRequest> loggedRequests = wireMockServer.findAll(newRequestPattern()
                                                             .withUrl(NOTIFICATION_URL)
                                                             .withRequestBody(
                                                                 matchingJsonPath(
                                                                     "$.personalisation.caseNumber",
                                                                     equalTo(caseReferenceWithDashes)
                                                                 )
                                                             )
        );

        return loggedRequests.stream()
            .filter(loggedRequest -> loggedRequest.getLoggedDate().toInstant().isAfter(sentSince))
            .toList();
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

}
