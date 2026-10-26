package uk.gov.hmcts.reform.pcs.bulkprint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.bulkprint.model.Letter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType;
import wiremock.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import wiremock.com.fasterxml.jackson.databind.node.ObjectNode;
import wiremock.com.fasterxml.jackson.databind.node.TextNode;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockSendLetterServer {

    private final WireMockServer wireMockServer;
    private final ObjectMapper objectMapper;
    private final CaseReferenceFormatter caseReferenceFormatter;

    @PostConstruct
    public void stubEndpoints() {
        stubSendLetterEndpoints();
    }

    public List<Letter> getSentLetters(long caseReference, LetterType letterType, int minLetterCount) {
        log.info("Waiting for {} letters of type {}", minLetterCount, letterType);

        waitForMinLetterCount(caseReference, letterType, minLetterCount);

        return getLoggedSendLetterRequests(caseReference, letterType).stream()
            .map(this::toLetter)
            .toList();
    }

    private void waitForMinLetterCount(long caseReference, LetterType letterType, int minLetterCount) {
        await("At least %d letter(s) of type %s sent".formatted(minLetterCount, letterType))
            .atMost(Duration.ofSeconds(20))
            .pollInterval(Duration.ofMillis(500))
            .ignoreExceptions()
            .until(() -> getSentLetterCount(caseReference, letterType), greaterThanOrEqualTo(minLetterCount));
    }

    private int getSentLetterCount(long caseReference, LetterType letterType) {
        return getLoggedSendLetterRequests(caseReference, letterType).size();
    }

    private List<LoggedRequest> getLoggedSendLetterRequests(long caseReference, LetterType letterType) {
        String caseReferenceWithDashes = caseReferenceFormatter.formatCaseReferenceWithDashes(caseReference);

        return wireMockServer
            .findAll(RequestPatternBuilder
                         .newRequestPattern(POST, urlPathEqualTo("/letters"))
                         .withRequestBody(
                             matchingJsonPath("$.additional_data.caseReference", equalTo(caseReferenceWithDashes))
                              .and(matchingJsonPath("$.type", equalTo(letterType.getCode())))
                         )
            );

    }

    private Letter toLetter(LoggedRequest loggedRequest) {
        String jsonBody = loggedRequest.getBodyAsString();
        try {
            return objectMapper.readValue(jsonBody, Letter.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void stubSendLetterEndpoints() {
        wireMockServer
            .stubFor(WireMock.get(urlPathMatching("/letters/.+"))
                         .willReturn(WireMock.ok().withJsonBody(emptyResponse())));

        wireMockServer
            .stubFor(WireMock.post(urlPathEqualTo("/letters"))
                         .willReturn(WireMock.created().withJsonBody(createdLetterResponse())));
    }

    private static ObjectNode emptyResponse() {
        return JsonNodeFactory.instance.objectNode();
    }

    private static ObjectNode createdLetterResponse() {
        ObjectNode jsonBody = JsonNodeFactory.instance.objectNode();
        jsonBody.set("letter_id", TextNode.valueOf(UUID.randomUUID().toString()));
        return jsonBody;
    }

}
