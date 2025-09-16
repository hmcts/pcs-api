//package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;
//
//import com.github.kagkarlsson.scheduler.SchedulerClient;
//import com.github.kagkarlsson.scheduler.task.Task;
//import com.github.kagkarlsson.scheduler.task.TaskInstance;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.ResponseEntity;
//import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
//import uk.gov.hmcts.reform.pcs.document.service.exception.DocAssemblyException;
//import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
//import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
//import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;
//import uk.gov.hmcts.reform.pcs.testingsupport.model.DocAssemblyRequest;
//
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.any;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class TestingSupportControllerTest {
//
//    @Mock
//    private SchedulerClient schedulerClient;
//    @Mock
//    private Task<Void> helloWorldTask;
//    @Mock
//    private DocAssemblyService docAssemblyService;
//    @Mock
//    private EligibilityService eligibilityService;
//
//    private TestingSupportController underTest;
//
//    @BeforeEach
//    void setUp() {
//        underTest = new TestingSupportController(schedulerClient, helloWorldTask,
//                                                 docAssemblyService, eligibilityService
//        );
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    void testScheduleHelloWorldTask_Success() {
//        TaskInstance<Void> mockTaskInstance = mock(TaskInstance.class);
//        when(helloWorldTask.instance(anyString())).thenReturn(mockTaskInstance);
//
//        ResponseEntity<String> response = underTest.scheduleHelloWorldTask(5,
//                                                                        "Bearer token",
//                                                                        "ServiceAuthToken");
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
//        assertThat(response.getBody()).contains("Hello World task scheduled successfully with ID:");
//        assertThat(response.getBody()).contains("execution time:");
//
//        ArgumentCaptor<TaskInstance<Void>> taskInstanceCaptor = ArgumentCaptor.forClass(TaskInstance.class);
//        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
//
//        verify(schedulerClient).scheduleIfNotExists(taskInstanceCaptor.capture(), instantCaptor.capture());
//
//        TaskInstance<Void> capturedTaskInstance = taskInstanceCaptor.getValue();
//        Instant scheduledInstant = instantCaptor.getValue();
//
//        assertThat(capturedTaskInstance).isSameAs(mockTaskInstance);
//        assertThat(scheduledInstant).isAfterOrEqualTo(Instant.now().plusSeconds(4));
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    void testScheduleHelloWorldTask_Failure() {
//        TaskInstance<Void> mockTaskInstance = mock(TaskInstance.class);
//        when(helloWorldTask.instance(anyString())).thenReturn(mockTaskInstance);
//
//        doThrow(new RuntimeException("Scheduler failure")).when(schedulerClient)
//            .scheduleIfNotExists(any(TaskInstance.class), any(Instant.class));
//
//        ResponseEntity<String> response = underTest.scheduleHelloWorldTask(2,
//                                                                        "Bearer token",
//                                                                        "ServiceAuthToken");
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
//        assertThat(response.getBody()).contains("Failed to schedule Hello World task");
//        assertThat(response.getBody()).contains("Scheduler failure");
//    }
//
//    @Test
//    void testGenerateDocument_WithBasicCaseInformation() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//
//        // Basic case information only
//        formPayload.put("ccdCaseReference", "PCS-123456789");
//        formPayload.put("referenceNumber", "REF-2024-001");
//        formPayload.put("caseName", "Smith v Jones");
//        formPayload.put("applicantExternalReference", "APP-REF-001");
//        formPayload.put("respondentExternalReference", "RESP-REF-001");
//        formPayload.put("issueDate", "2024-01-15");
//        formPayload.put("submittedOn", "2024-01-10");
//        formPayload.put(
//            "descriptionOfClaim",
//            "The claimant seeks possession of the property due to non-payment of rent.");
//
//        request.setFormPayload(formPayload);
//
//        String expectedDocumentUrl = "http://dm-store/documents/123";
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn(expectedDocumentUrl);
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(201);
//        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
//        assertThat(response.getHeaders().getLocation()).isEqualTo(java.net.URI.create(expectedDocumentUrl));
//
//        // Verify the request was passed correctly
//        ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
//        verify(docAssemblyService).generateDocument(requestCaptor.capture());
//
//        DocAssemblyRequest capturedRequest = requestCaptor.getValue();
//        assertThat(capturedRequest.getFormPayload()).containsKey("ccdCaseReference");
//        assertThat(capturedRequest.getFormPayload()).containsKey("caseName");
//        assertThat(capturedRequest.getFormPayload()).containsKey("descriptionOfClaim");
//        assertThat(capturedRequest.getTemplateId()).isNull();
//    }
//
//    @Test
//    void testGenerateDocument_WithCustomTemplateId() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("ccdCaseReference", "PCS-123456789");
//        formPayload.put("caseName", "Test Case");
//        formPayload.put("descriptionOfClaim", "Test claim description");
//
//        request.setFormPayload(formPayload);
//        request.setTemplateId("CUSTOM-TEMPLATE-123.docx");
//        request.setOutputType("DOCX");
//
//        String expectedDocumentUrl = "http://dm-store/documents/456";
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn(expectedDocumentUrl);
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(201);
//        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
//        assertThat(response.getHeaders().getLocation()).isEqualTo(java.net.URI.create(expectedDocumentUrl));
//
//        // Verify the request was passed correctly with custom template
//        ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
//        verify(docAssemblyService).generateDocument(requestCaptor.capture());
//
//        DocAssemblyRequest capturedRequest = requestCaptor.getValue();
//        assertThat(capturedRequest.getTemplateId()).isEqualTo("CUSTOM-TEMPLATE-123.docx");
//        assertThat(capturedRequest.getOutputType()).isEqualTo("DOCX");
//        assertThat(capturedRequest.getFormPayload()).containsKey("ccdCaseReference");
//    }
//
//    @Test
//    void testGenerateDocument_WithEmptyTemplateId() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("ccdCaseReference", "PCS-123456789");
//        formPayload.put("caseName", "Test Case");
//        formPayload.put("descriptionOfClaim", "Test claim description");
//
//        request.setFormPayload(formPayload);
//        request.setTemplateId(""); // Empty template ID should use default
//
//        String expectedDocumentUrl = "http://dm-store/documents/789";
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn(expectedDocumentUrl);
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(201);
//        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
//        assertThat(response.getHeaders().getLocation()).isEqualTo(java.net.URI.create(expectedDocumentUrl));
//
//        // Verify the request was passed correctly with empty template ID
//        ArgumentCaptor<DocAssemblyRequest> requestCaptor = ArgumentCaptor.forClass(DocAssemblyRequest.class);
//        verify(docAssemblyService).generateDocument(requestCaptor.capture());
//
//        DocAssemblyRequest capturedRequest = requestCaptor.getValue();
//        assertThat(capturedRequest.getTemplateId()).isEmpty();
//        assertThat(capturedRequest.getFormPayload()).containsKey("ccdCaseReference");
//    }
//
//    @Test
//    void testGenerateDocument_Success() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("ccdCaseReference", "PCS-123456789");
//        formPayload.put("caseName", "Test Case");
//        formPayload.put("descriptionOfClaim", "Test claim description");
//        request.setFormPayload(formPayload);
//
//        String expectedDocumentUrl = "http://dm-store/documents/123";
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn(expectedDocumentUrl);
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(201);
//        assertThat(response.getBody()).isEqualTo(expectedDocumentUrl);
//        assertThat(response.getHeaders().getLocation()).isEqualTo(java.net.URI.create(expectedDocumentUrl));
//        verify(docAssemblyService).generateDocument(request);
//    }
//
//    @Test
//    void testGenerateDocument_Failure() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("field1", "value1");
//        request.setFormPayload(formPayload);
//
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenThrow(new RuntimeException("Document generation failed"));
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
//        assertThat(response.getBody()).contains("An error occurred while processing your request.");
//    }
//
//    @Test
//    void testGenerateDocument_NullRequest() {
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", null);
//        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
//
//        assertThat(response.getBody()).contains("Doc Assembly service returned invalid document URL");
//
//    }
//
//    @Test
//    void testGenerateDocument_NullAuthorization() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        request.setFormPayload(new HashMap<>());
//
//        // Mock the service to return null, which triggers the invalid document URL error
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn(null);
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
//        assertThat(response.getBody()).contains("Doc Assembly service returned invalid document URL");
//
//    }
//
//    @Test
//    void testGenerateDocument_EmptyAuthorization() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        request.setFormPayload(new HashMap<>());
//        // Mock the service to return empty string, which triggers the invalid document URL error
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn("");
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
//        assertThat(response.getBody()).contains("Doc Assembly service returned invalid document URL");
//    }
//
//    @Test
//    void testGenerateDocument_WhitespaceAuthorization() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        request.setFormPayload(new HashMap<>());
//
//        // Mock the service to return whitespace, which triggers the invalid document URL error
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn("   ");
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
//        assertThat(response.getBody()).contains("Doc Assembly service returned invalid document URL");
//
//    }
//
//    @Test
//    void testGenerateDocument_NullServiceAuthorization() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        request.setFormPayload(new HashMap<>());
//
//        // Mock the service to return null, which triggers the invalid document URL error
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn(null);
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
//        assertThat(response.getBody()).contains("Doc Assembly service returned invalid document URL");
//    }
//
//    @Test
//    void testGenerateDocument_EmptyServiceAuthorization() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        request.setFormPayload(new HashMap<>());
//
//        // Mock the service to return empty string, which triggers the invalid document URL error
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn("");
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
//        assertThat(response.getBody()).contains("Doc Assembly service returned invalid document URL");
//
//    }
//
//    @Test
//    void testGenerateDocument_WhitespaceServiceAuthorization() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        request.setFormPayload(new HashMap<>());
//
//        // Mock the service to return whitespace, which triggers the invalid document URL error
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenReturn("   ");
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
//        assertThat(response.getBody()).contains("Doc Assembly service returned invalid document URL");
//    }
//
//    @Test
//    void testGenerateDocument_DocAssemblyBadRequestException() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("field1", "value1");
//        request.setFormPayload(formPayload);
//
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenThrow(new DocAssemblyException("Bad request to Doc Assembly service: Invalid template"));
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(400);
//        assertThat(response.getBody()).contains("Bad request to Doc Assembly service: Invalid template");
//    }
//
//    @Test
//    void testGenerateDocument_DocAssemblyAuthorizationException() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("field1", "value1");
//        request.setFormPayload(formPayload);
//
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenThrow(new DocAssemblyException(
//                "Authorization failed for Doc Assembly service: Unauthorized"));
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(401);
//        assertThat(response.getBody()).contains(
//            "Authorization failed: Authorization failed for Doc Assembly service: Unauthorized");
//    }
//
//    @Test
//    void testGenerateDocument_DocAssemblyNotFoundException() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("field1", "value1");
//        request.setFormPayload(formPayload);
//
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenThrow(new DocAssemblyException(
//                "Doc Assembly service endpoint not found: Not found"));
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(404);
//        assertThat(response.getBody()).contains(
//            "Doc Assembly service endpoint not found: Doc Assembly service endpoint not found: Not found");
//    }
//
//    @Test
//    void testGenerateDocument_DocAssemblyServiceUnavailableException() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("field1", "value1");
//        request.setFormPayload(formPayload);
//
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenThrow(new DocAssemblyException(
//                "Doc Assembly service is temporarily unavailable: Service unavailable"));
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(503);
//        assertThat(response.getBody()).contains(
//            "Doc Assembly service is temporarily unavailable: "
//            + "Doc Assembly service is temporarily unavailable: Service unavailable");
//    }
//
//    @Test
//    void testGenerateDocument_DocAssemblyServiceErrorException() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("field1", "value1");
//        request.setFormPayload(formPayload);
//
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenThrow(new DocAssemblyException("Doc Assembly service error: Internal server error"));
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(503);
//        assertThat(response.getBody()).contains(
//            "Doc Assembly service is temporarily unavailable: Doc Assembly service error: Internal server error");
//    }
//
//    @Test
//    void testGenerateDocument_DocAssemblyGenericException() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("field1", "value1");
//        request.setFormPayload(formPayload);
//
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenThrow(new DocAssemblyException("Some other error occurred"));
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(500);
//        assertThat(response.getBody()).contains("Doc Assembly service error: Some other error occurred");
//    }
//
//    @Test
//    void testGenerateDocument_IllegalArgumentException() {
//        final DocAssemblyRequest request = new DocAssemblyRequest();
//        Map<String, Object> formPayload = new HashMap<>();
//        formPayload.put("field1", "value1");
//        request.setFormPayload(formPayload);
//
//        when(docAssemblyService.generateDocument(any(DocAssemblyRequest.class)))
//            .thenThrow(new IllegalArgumentException("Request cannot be null"));
//
//        ResponseEntity<String> response = underTest.generateDocument("test-auth", "test-s2s", request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatusCode().value()).isEqualTo(400);
//        assertThat(response.getBody()).contains("Invalid request: Request cannot be null");
//    }
//
//    @Test
//    void shouldDelegateToEligibilityServiceAndReturnResult() {
//        // Given
//        String serviceAuth = "Bearer serviceToken";
//        String postcode = "SW1A 1AA";
//        LegislativeCountry country = LegislativeCountry.ENGLAND;
//
//        EligibilityResult expectedResult = mock(EligibilityResult.class);
//
//        when(eligibilityService.checkEligibility(postcode, country)).thenReturn(expectedResult);
//
//        // When
//        EligibilityResult result = underTest.getPostcodeEligibility(serviceAuth, postcode, country);
//
//        // Then
//        assertThat(result).isSameAs(expectedResult);
//        verify(eligibilityService).checkEligibility(postcode, country);
//    }
//
//    @Test
//    void shouldDelegateToEligibilityServiceWhenCountryNotSpecified() {
//        // Given
//        String serviceAuth = "Bearer serviceToken";
//        String postcode = "CH5 1AA";
//
//        EligibilityResult expectedResult = mock(EligibilityResult.class);
//
//        when(eligibilityService.checkEligibility(postcode, null)).thenReturn(expectedResult);
//
//        // When
//        EligibilityResult result = underTest.getPostcodeEligibility(serviceAuth, postcode, null);
//
//        // Then
//        assertThat(result).isSameAs(expectedResult);
//        verify(eligibilityService).checkEligibility(postcode, null);
//    }
//
//    @Test
//    void shouldPropagateExceptionWhenEligibilityServiceFails() {
//        // Given
//        String serviceAuth = "Bearer serviceToken";
//        String postcode = "INVALID";
//        RuntimeException serviceException = new RuntimeException("Service error");
//
//        when(eligibilityService.checkEligibility(postcode, null)).thenThrow(serviceException);
//
//        // When/Then
//        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
//            underTest.getPostcodeEligibility(serviceAuth, postcode, null)
//        ).isInstanceOf(RuntimeException.class)
//         .hasMessageContaining("Service error");
//
//        verify(eligibilityService).checkEligibility(postcode, null);
//    }
//}
