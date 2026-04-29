package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyAccessCodeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAccessCodeRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;
import uk.gov.hmcts.reform.pcs.testingsupport.service.CcdTestCaseOrchestrator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private EligibilityService eligibilityService;
    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private PartyAccessCodeRepository partyAccessCodeRepository;
    @Mock
    private CcdTestCaseOrchestrator ccdTestCaseOrchestrator;
    @Mock
    private ModelMapper modelMapper;


    private TestingSupportController underTest;

    @BeforeEach
    void setUp() {
        underTest = new TestingSupportController(schedulerClient, helloWorldTask,
                                                 eligibilityService,
                                                 pcsCaseRepository, partyAccessCodeRepository,
                                                 modelMapper, ccdTestCaseOrchestrator
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
        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
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
        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).contains("Failed to schedule Hello World task");
        assertThat(response.getBody()).contains("Scheduler failure");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testScheduleHelloWorldTask_WithDefaultDelaySeconds() {
        TaskInstance<Void> mockTaskInstance = mock(TaskInstance.class);
        when(helloWorldTask.instance(anyString())).thenReturn(mockTaskInstance);

        Instant testStartTime = Instant.now();
        ResponseEntity<String> response = underTest.scheduleHelloWorldTask(1,
                                                                           "Bearer token",
                                                                           "ServiceAuthToken");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Hello World task scheduled successfully with ID:");

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(schedulerClient).scheduleIfNotExists(any(TaskInstance.class), instantCaptor.capture());

        Instant scheduledInstant = instantCaptor.getValue();
        assertThat(scheduledInstant)
            .isAfterOrEqualTo(testStartTime)
            .isBeforeOrEqualTo(testStartTime.plusSeconds(2));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testScheduleHelloWorldTask_WithDefaultAuthorization() {
        TaskInstance<Void> mockTaskInstance = mock(TaskInstance.class);
        when(helloWorldTask.instance(anyString())).thenReturn(mockTaskInstance);

        ResponseEntity<String> response = underTest.scheduleHelloWorldTask(3,
                                                                           "DummyId",
                                                                           "ServiceAuthToken");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Hello World task scheduled successfully with ID:");
        verify(schedulerClient).scheduleIfNotExists(any(TaskInstance.class), any(Instant.class));
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
        Assertions.assertThatThrownBy(() ->
            underTest.getPostcodeEligibility(serviceAuth, postcode, null)
        ).isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Service error");

        verify(eligibilityService).checkEligibility(postcode, null);
    }

    @Test
    void shouldReturnEligibilityResultWithWalesCountry() {
        // Given
        String serviceAuth = "Bearer serviceToken";
        String postcode = "CF10 1AA";
        LegislativeCountry country = LegislativeCountry.WALES;

        EligibilityResult expectedResult = mock(EligibilityResult.class);
        when(eligibilityService.checkEligibility(postcode, country)).thenReturn(expectedResult);

        // When
        EligibilityResult result = underTest.getPostcodeEligibility(serviceAuth, postcode, country);

        // Then
        assertThat(result).isSameAs(expectedResult);
        verify(eligibilityService).checkEligibility(postcode, country);
    }

    @Test
    void shouldReturnPins() {
        // Given
        long caseReference = 8888888888888888L;
        UUID caseId = UUID.randomUUID();
        String accessCodeString = "CODE123";
        UUID partyCode = UUID.randomUUID();
        String firstName = "firstname";
        String lastName = "lastname";

        PartyEntity defendant = PartyEntity.builder()
            .id(partyCode)
            .firstName(firstName)
            .lastName(lastName)
            .addressKnown(VerticalYesNo.NO)
            .build();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder()
            .id(caseId)
            .caseReference(caseReference)
            .parties(Set.of(defendant))
            .build();

        List<PartyAccessCodeEntity> accessCodes = new ArrayList<>();
        PartyAccessCodeEntity accessCode1 = PartyAccessCodeEntity.builder()
            .id(UUID.randomUUID())
            .code(accessCodeString)
            .partyId(partyCode)
            .build();
        accessCodes.add(accessCode1);

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.ofNullable(caseEntity));

        when(partyAccessCodeRepository.findAllByPcsCase_Id(caseId))
            .thenReturn(accessCodes);

        // When
        ResponseEntity<Map<String, Party>> response = underTest.getPins(
            "ServiceAuthToken", caseReference
        );

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertNotNull(response.getBody());
        assertThat(
            response.getBody().get(accessCodeString).getFirstName().equals(firstName)
                &&
                response.getBody().get(accessCodeString).getLastName().equals(lastName));
    }

    @Test
    void shouldHandleInvalidCaseWhenReturnPins() {
        // Given
        long caseReference = 8888888888888888L;

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.empty());

        // When
        ResponseEntity<Map<String, Party>> response = underTest.getPins(
            "ServiceAuthToken", caseReference
        );

        // Then
        assertThat(HttpStatus.NOT_FOUND.equals(response.getStatusCode()));
    }

    @Test
    void shouldHandleServerErrorWhenReturnPins() {
        // Given
        long caseReference = 1111111111111111L;

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenThrow(new RuntimeException());

        // When
        ResponseEntity<Map<String, Party>> response = underTest.getPins(
            "ServiceAuthToken", caseReference
        );

        // Then
        assertThat(HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getStatusCode()));
    }

}
