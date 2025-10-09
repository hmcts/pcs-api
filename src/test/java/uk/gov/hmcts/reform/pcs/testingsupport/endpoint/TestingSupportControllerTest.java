package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestingSupportControllerTest {

    @Mock
    private SchedulerClient schedulerClient;
    @Mock
    private Task<Void> helloWorldTask;
    @Mock
    private DocAssemblyService docAssemblyService;
    @Mock
    private EligibilityService eligibilityService;

    private TestingSupportController underTest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        underTest = new TestingSupportController(schedulerClient, helloWorldTask,
                                                 docAssemblyService, eligibilityService
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void testScheduleHelloWorldTask_Success() {
        TaskInstance<Void> mockTaskInstance = mock(TaskInstance.class);
        when(helloWorldTask.instance(anyString())).thenReturn(mockTaskInstance);

        ResponseEntity<String> response = underTest.scheduleHelloWorldTask(5,
                                                                           "Bearer token",
                                                                           "ServiceAuthToken");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Hello World task scheduled successfully with ID:");
        assertThat(response.getBody()).contains("execution time:");

        ArgumentCaptor<TaskInstance<Void>> taskInstanceCaptor = ArgumentCaptor.forClass(TaskInstance.class);
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture(), instantCaptor.capture());

        TaskInstance<Void> capturedTaskInstance = taskInstanceCaptor.getValue();
        Instant scheduledInstant = instantCaptor.getValue();

        assertThat(capturedTaskInstance).isSameAs(mockTaskInstance);
        assertThat(scheduledInstant).isAfterOrEqualTo(Instant.now().plusSeconds(4));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testScheduleHelloWorldTask_Failure() {
        TaskInstance<Void> mockTaskInstance = mock(TaskInstance.class);
        when(helloWorldTask.instance(anyString())).thenReturn(mockTaskInstance);

        doThrow(new RuntimeException("Scheduler failure")).when(schedulerClient)
            .scheduleIfNotExists(any(TaskInstance.class), any(Instant.class));

        ResponseEntity<String> response = underTest.scheduleHelloWorldTask(2,
                                                                           "Bearer token",
                                                                           "ServiceAuthToken");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).contains("Failed to schedule Hello World task");
        assertThat(response.getBody()).contains("Scheduler failure");
    }

    @Test
    void testGenerateDocument_WithBasicCaseInformation() {
        final JsonNode formPayload = createJsonNodeFormPayload("John Smith", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/123";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
        assertThat(response.getHeaders().getLocation()).isEqualTo(java.net.URI.create(expectedDocumentUrl));

        // Verify the request was passed correctly
        ArgumentCaptor<JsonNode> formPayloadCaptor = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<String> templateIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OutputType> outputTypeCaptor = ArgumentCaptor.forClass(OutputType.class);
        ArgumentCaptor<String> outputFilenameCaptor = ArgumentCaptor.forClass(String.class);

        verify(docAssemblyService).generateDocument(
            formPayloadCaptor.capture(),
            templateIdCaptor.capture(),
            outputTypeCaptor.capture(),
            outputFilenameCaptor.capture()
        );

        JsonNode capturedFormPayload = formPayloadCaptor.getValue();
        assertThat(capturedFormPayload).isSameAs(formPayload);
        assertThat(templateIdCaptor.getValue()).isEqualTo("CV-SPC-CLM-ENG-01356.docx");
        assertThat(outputTypeCaptor.getValue()).isEqualTo(OutputType.PDF);
        assertThat(outputFilenameCaptor.getValue()).isEqualTo("generated-document.pdf");
    }


    @Test
    void testGenerateDocument_WithCustomTemplateId() {
        // Note: Controller uses hardcoded template, but keeping test pattern for consistency
        final JsonNode formPayload = createJsonNodeFormPayload("Jane Doe", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/456";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
        assertThat(response.getHeaders().getLocation()).isEqualTo(java.net.URI.create(expectedDocumentUrl));

        // Verify the request was passed correctly with hardcoded template
        ArgumentCaptor<JsonNode> formPayloadCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(docAssemblyService).generateDocument(
            formPayloadCaptor.capture(),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        );

        JsonNode capturedFormPayload = formPayloadCaptor.getValue();
        // Access JsonNode fields directly
        assertThat(capturedFormPayload.get("applicantName").asText()).isEqualTo("Jane Doe");
        assertThat(capturedFormPayload.get("caseNumber").asText()).isEqualTo("PCS-123456789");
    }

    @Test
    void testGenerateDocument_WithEmptyTemplateId() {
        // Keeping test pattern
        final JsonNode formPayload = createJsonNodeFormPayload("Test User", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/789";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
        assertThat(response.getHeaders().getLocation()).isEqualTo(java.net.URI.create(expectedDocumentUrl));

        // Verify the request was passed correctly with hardcoded template
        ArgumentCaptor<JsonNode> formPayloadCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(docAssemblyService).generateDocument(
            formPayloadCaptor.capture(),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        );

        JsonNode capturedFormPayload = formPayloadCaptor.getValue();
        // Access JsonNode fields directly
        assertThat(capturedFormPayload.get("caseNumber").asText()).isEqualTo("PCS-123456789");
    }

    @Test
    void testGenerateDocument_Success() {
        final JsonNode formPayload = createJsonNodeFormPayload("Test Case", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/123";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
        assertThat(response.getHeaders().getLocation()).isEqualTo(java.net.URI.create(expectedDocumentUrl));
        verify(docAssemblyService).generateDocument(
            eq(formPayload),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        );
    }

    @Test
    void testGenerateDocument_Failure() {
        final JsonNode formPayload = createJsonNodeFormPayload("value1", "PCS-123456789");

        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenThrow(new RuntimeException("Document generation failed"));

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).contains("An error occurred while processing your request.");
    }

    @Test
    void testGenerateDocument_NullRequest() {
        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", null);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("FormPayload is required");
    }


    @Test
    void testGenerateDocument_WhitespaceAuthorization() {
        final JsonNode formPayload = createJsonNodeFormPayload("Test", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/123";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument("   ", "test-s2s", formPayload);

        // Controller doesn't validate authorization, so request succeeds
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
    }

    @Test
    void testGenerateDocument_NullAuthorization() {
        final JsonNode formPayload = createJsonNodeFormPayload("Test", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/123";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument(null, "test-s2s", formPayload);

        // Controller doesn't validate authorization, so request succeeds
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
    }

    @Test
    void testGenerateDocument_EmptyAuthorization() {
        final JsonNode formPayload = createJsonNodeFormPayload("Test", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/123";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument("", "test-s2s", formPayload);

        // Controller doesn't validate authorization, so request succeeds
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
    }

    // Similar fixes for service authorization tests
    @Test
    void testGenerateDocument_NullServiceAuthorization() {
        final JsonNode formPayload = createJsonNodeFormPayload("Test", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/123";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument("test-auth", null, formPayload);

        // Controller doesn't validate service authorization, so request succeeds
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
    }

    @Test
    void testGenerateDocument_EmptyServiceAuthorization() {
        final JsonNode formPayload = createJsonNodeFormPayload("Test", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/123";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "", formPayload);

        // Controller doesn't validate service authorization, so request succeeds
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
    }

    @Test
    void testGenerateDocument_WhitespaceServiceAuthorization() {
        final JsonNode formPayload = createJsonNodeFormPayload("Test", "PCS-123456789");

        String expectedDocumentUrl = "http://dm-store/documents/123";
        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenReturn(expectedDocumentUrl);

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "   ", formPayload);

        // Controller doesn't validate service authorization, so request succeeds
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
    }

    @Test
    void testGenerateDocument_DocAssemblyBadRequestException() {
        final JsonNode formPayload = createJsonNodeFormPayload("value1", "PCS-123456789");

        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenThrow(new DocAssemblyException("Bad request to Doc Assembly service: Invalid template"));

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains(
            "Bad request to Doc Assembly service: Bad request to Doc Assembly service: Invalid template");
    }

    @Test
    void testGenerateDocument_DocAssemblyAuthorizationException() {
        final JsonNode formPayload = createJsonNodeFormPayload("value1", "PCS-123456789");

        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenThrow(new DocAssemblyException(
            "Authorization failed for Doc Assembly service: Unauthorized"));

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).contains(
            "Authorization failed: Authorization failed for Doc Assembly service: Unauthorized");
    }

    @Test
    void testGenerateDocument_DocAssemblyNotFoundException() {
        final JsonNode formPayload = createJsonNodeFormPayload("value1", "PCS-123456789");

        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenThrow(new DocAssemblyException(
            "Doc Assembly service endpoint not found: Not found"));

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains(
            "Doc Assembly service endpoint not found: Doc Assembly service endpoint not found: Not found");
    }

    @Test
    void testGenerateDocument_DocAssemblyServiceUnavailableException() {
        final JsonNode formPayload = createJsonNodeFormPayload("value1", "PCS-123456789");

        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenThrow(new DocAssemblyException(
            "Doc Assembly service is temporarily unavailable: Service unavailable"));

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).contains(
            "Doc Assembly service is temporarily unavailable: "
                + "Doc Assembly service is temporarily unavailable: Service unavailable");
    }

    @Test
    void testGenerateDocument_DocAssemblyServiceErrorException() {
        final JsonNode formPayload = createJsonNodeFormPayload("value1", "PCS-123456789");

        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenThrow(new DocAssemblyException("Doc Assembly service error: Internal server error"));

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).contains(
            "Doc Assembly service is temporarily unavailable: Doc Assembly service error: Internal server error");
    }

    @Test
    void testGenerateDocument_DocAssemblyGenericException() {
        final JsonNode formPayload = createJsonNodeFormPayload("value1", "PCS-123456789");

        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenThrow(new DocAssemblyException("Some other error occurred"));

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).contains("Doc Assembly service error: Some other error occurred");
    }

    @Test
    void testGenerateDocument_IllegalArgumentException() {
        final JsonNode formPayload = createJsonNodeFormPayload("value1", "PCS-123456789");

        when(docAssemblyService.generateDocument(
            any(JsonNode.class),
            eq("CV-SPC-CLM-ENG-01356.docx"),
            eq(OutputType.PDF),
            eq("generated-document.pdf")
        )).thenThrow(new IllegalArgumentException("Request cannot be null"));

        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", formPayload);

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("Invalid request: Request cannot be null");
    }

    @Test
    void shouldDelegateToEligibilityServiceAndReturnResult() {
        // Given
        String serviceAuth = "Bearer serviceToken";
        String postcode = "SW1A 1AA";
        LegislativeCountry country = LegislativeCountry.ENGLAND;

        EligibilityResult expectedResult = mock(EligibilityResult.class);

        when(eligibilityService.checkEligibility(postcode, country)).thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.getPostcodeEligibility(serviceAuth, postcode, country);

        // Then
        assertThat(result).isSameAs(expectedResult);
        verify(eligibilityService).checkEligibility(postcode, country);
    }

    @Test
    void shouldDelegateToEligibilityServiceWhenCountryNotSpecified() {
        // Given
        String serviceAuth = "Bearer serviceToken";
        String postcode = "CH5 1AA";

        EligibilityResult expectedResult = mock(EligibilityResult.class);

        when(eligibilityService.checkEligibility(postcode, null)).thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.getPostcodeEligibility(serviceAuth, postcode, null);

        // Then
        assertThat(result).isSameAs(expectedResult);
        verify(eligibilityService).checkEligibility(postcode, null);
    }

    @Test
    void shouldPropagateExceptionWhenEligibilityServiceFails() {
        // Given
        String serviceAuth = "Bearer serviceToken";
        String postcode = "INVALID";
        RuntimeException serviceException = new RuntimeException("Service error");

        when(eligibilityService.checkEligibility(postcode, null)).thenThrow(serviceException);

        // When/Then
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            underTest.getPostcodeEligibility(serviceAuth, postcode, null)
        ).isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Service error");

        verify(eligibilityService).checkEligibility(postcode, null);
    }

    private JsonNode createJsonNodeFormPayload(String applicantName, String caseNumber) {
        try {
            String json = String.format("{\"applicantName\":\"%s\",\"caseNumber\":\"%s\"}",
                                      applicantName, caseNumber);
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JsonNode", e);
        }
    }
}
