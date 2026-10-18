package uk.gov.hmcts.reform.pcs.enforcement.service;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.reform.ccd.client.CaseAssignmentApi;
import uk.gov.hmcts.reform.payments.client.PaymentsClient;
import uk.gov.hmcts.reform.payments.request.CreateServiceRequestDTO;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.feeandpay.FeePaymentRepository;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;
import uk.gov.hmcts.reform.pcs.config.IssuedCases;
import uk.gov.hmcts.reform.pcs.config.PcsCcdEventTest;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.common.StatementOfTruthDetailsEnforcement;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.enforcement.domain.writ.WritDetails;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementEmail;
import uk.gov.hmcts.reform.pcs.enforcement.notification.EnforcementNotificationSender;
import uk.gov.hmcts.reform.pcs.enforcement.repository.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.enforcement.testcasesupport.EnforcementFeePaidSimulator;
import uk.gov.hmcts.reform.pcs.feesandpay.exception.FeeNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentCallbackStrategyFactory;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pcs.feesandpay.endpoint.PaymentCallBackController.PAYMENT_UPDATE_PATH;
import static uk.gov.hmcts.reform.pcs.feesandpay.endpoint.PaymentCallBackController.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.pcs.feesandpay.task.FeesAndPayTaskComponent.FEES_AND_PAY_TASK_DESCRIPTOR;

/**
 * An enforcement fee taken through PCS's standard payments pattern, against Postgres: the claimant submits
 * "Enforce the order" through the SDK's real submit path, the fees-and-pay task raises the Payments service
 * request, and Payments' callback to {@code PaymentCallBackController} moves the enforcement on. The Fees
 * Register and Payments are mocked.
 *
 * <p>This context polls for db-scheduler tasks hourly, so the test runs the fees-and-pay task's work itself from
 * the task data the submit scheduled, then cancels the task so that another test context's scheduler does not
 * run it against a real Payments.</p>
 */
@PcsCcdEventTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "db-scheduler.polling-interval-seconds=3600")
@DisplayName("Enforcement fee payment")
class EnforcementPaymentIT extends AbstractPostgresContainerIT {

    private static final String DOCUMENT_TASK_NAME = "enforcement-document-generation-task";
    private static final FeeDetails WARRANT_FEE = FeeDetails.builder()
        .code("FEE0380")
        .description("Issue of a warrant of possession/warrant of delivery")
        .feeAmount(new BigDecimal("148.00"))
        .version(5)
        .build();
    private static final FeeDetails WRIT_FEE = FeeDetails.builder()
        .code("FEE0397")
        .description("Seal a writ of possession")
        .feeAmount(new BigDecimal("78.00"))
        .version(3)
        .build();

    @Autowired
    private CcdEventTestSupport<PCSCase, State> events;
    @Autowired
    private IssuedCases cases;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PcsCaseRepository pcsCaseRepository;
    @Autowired
    private EnforcementOrderRepository enforcementOrderRepository;
    @Autowired
    private FeePaymentRepository feePaymentRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentCallbackStrategyFactory paymentCallbackStrategyFactory;
    @Autowired
    private EnforcementFeePaidSimulator enforcementFeePaidSimulator;
    @Autowired
    private SchedulerClient schedulerClient;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeeService feeService;
    @MockitoBean
    private PaymentsClient paymentsClient;
    @MockitoBean(name = "systemUpdateUserTokenProvider")
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @MockitoBean
    private EnforcementNotificationSender sender;
    /**
     * Deleting the claimant's draft keys it on their organisation, which comes from PRD.
     */
    @MockitoBean
    private OrganisationService organisationService;
    /**
     * A case view looks up the viewer's case roles to filter what defendants see (DefendantEnforcementView); the
     * SDK test support does not answer it, so the mock gives the viewer no case roles.
     */
    @MockitoBean
    private CaseAssignmentApi caseAssignmentApi;

    private final List<UUID> enforcementIds = new ArrayList<>();
    private long caseReference;
    private Actor claimantSolicitor;

    @BeforeEach
    void setUp() {
        caseReference = cases.issue();
        claimantSolicitor = events.registerActor("Clare", "Solicitor", "caseworker-pcs-solicitor");
        when(organisationService.requireOrganisationIdForCurrentUser()).thenReturn("ORG1234");
        when(feeService.getFee(FeeType.ENFORCEMENT_WARRANT_FEE)).thenReturn(WARRANT_FEE);
        when(feeService.getFee(FeeType.ENFORCEMENT_WRIT_FEE)).thenReturn(WRIT_FEE);
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("Bearer system-user");
    }

    /**
     * These cases commit, so remove the tasks they scheduled before another test context's scheduler runs them.
     */
    @AfterEach
    void removeScheduledTasks() {
        enforcementIds.forEach(id -> {
            jdbcTemplate.update("DELETE FROM scheduled_tasks WHERE task_name = ? AND task_instance = ?",
                                FEES_AND_PAY_TASK_DESCRIPTOR.getTaskName(), EnforcementFeeService.instanceId(id));
            jdbcTemplate.update("DELETE FROM scheduled_tasks WHERE task_name = ? AND task_instance LIKE ?",
                                DOCUMENT_TASK_NAME, id + "-%");
        });
    }

    @Test
    @DisplayName("every payment callback type has a handler, and enforcement's is the enforcement handler")
    void everyPaymentCallbackTypeHasAHandler() {
        for (PaymentCallbackHandlerType type : PaymentCallbackHandlerType.values()) {
            assertThat(paymentCallbackStrategyFactory.getStrategy(type)).as(type.name()).isNotNull();
        }
        assertThat(paymentCallbackStrategyFactory.getStrategy(PaymentCallbackHandlerType.ENFORCEMENT))
            .isInstanceOf(EnforcementPaymentCallbackHandler.class);
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT", "WRIT"})
    @DisplayName("submitting raises a service request for the fee, and paying it moves the enforcement to review")
    void submittingAndPayingTheFee(SelectEnforcementType enforcementType) throws Exception {
        FeeDetails fee = enforcementType == SelectEnforcementType.WARRANT ? WARRANT_FEE : WRIT_FEE;
        String feeAmount = enforcementType == SelectEnforcementType.WARRANT ? "£148" : "£78";

        var accepted = submit(enforcementType);

        assertThat(accepted.confirmationBody())
            .contains("Pay " + feeAmount + " for HM Courts and Tribunals Service")
            .contains("/cases/case-details/%d#Service%%20Request".formatted(caseReference));
        EnforcementOrderEntity enforcement = storedEnforcement();
        assertThat(enforcement.getStatus()).isEqualTo(EnforcementStatus.AWAITING_PAYMENT);
        FeesAndPayTaskData taskData = takeScheduledServiceRequest(enforcement.getId());
        assertThat(taskData.getFeeDetails()).isEqualTo(fee);
        assertThat(taskData.getCaseReference()).isEqualTo(caseReference);
        assertThat(taskData.getPaymentCallbackHandlerType()).isEqualTo(PaymentCallbackHandlerType.ENFORCEMENT);
        assertThat(taskData.getRelatedEntityId()).isEqualTo(enforcement.getId());
        assertThat(taskData.getResponsiblePartyId()).isEqualTo(claimantPartyId());
        assertThat(taskData.getResponsiblePartyName()).isEqualTo(IssuedCases.CLAIMANT);

        String serviceRequest = raiseServiceRequest(taskData);
        final String stateBeforePayment = events.snapshot(caseReference).state();
        paymentCallback(serviceRequest, PaymentStatus.PAID);

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_REVIEW);
        FeePaymentEntity feePayment = feePaymentRepository.findByServiceRequestReference(serviceRequest).orElseThrow();
        assertThat(feePayment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(feePayment.getParty()).isNull();
        assertThat(events.snapshot(caseReference).state()).isEqualTo(stateBeforePayment);
        verify(sender).send(caseReference, EnforcementEmail.forApplicationReceived(enforcementType), Map.of());
        assertThat(scheduledDocuments(enforcement.getId())).isNotEmpty();
    }

    @Test
    @DisplayName("a payment that failed leaves the enforcement awaiting payment")
    void failedPayment() throws Exception {
        submit(SelectEnforcementType.WARRANT);
        EnforcementOrderEntity enforcement = storedEnforcement();
        String serviceRequest = raiseServiceRequest(takeScheduledServiceRequest(enforcement.getId()));

        paymentCallback(serviceRequest, PaymentStatus.NOT_PAID);

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_PAYMENT);
        verify(sender, never()).send(anyLong(), any(), any());
        assertThat(scheduledDocuments(enforcement.getId())).isEmpty();
    }

    @Test
    @DisplayName("a second paid callback for the same fee changes nothing")
    void duplicatePaidCallback() throws Exception {
        submit(SelectEnforcementType.WARRANT);
        EnforcementOrderEntity enforcement = storedEnforcement();
        String serviceRequest = raiseServiceRequest(takeScheduledServiceRequest(enforcement.getId()));
        paymentCallback(serviceRequest, PaymentStatus.PAID);

        paymentCallback(serviceRequest, PaymentStatus.PAID);

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_REVIEW);
        verify(sender).send(caseReference, EnforcementEmail.WARRANT_APPLICATION_RECEIVED, Map.of());
    }

    @Test
    @DisplayName("a submit whose fee cannot be looked up is refused and saves nothing")
    void feeLookUpFailure() {
        when(feeService.getFee(FeeType.ENFORCEMENT_WARRANT_FEE))
            .thenThrow(new FeeNotFoundException("Unable to retrieve fee: ENFORCEMENT_WARRANT_FEE"));

        var rejected = events.event(caseReference, EventId.enforceTheOrder.name(),
                                    application(SelectEnforcementType.WARRANT))
            .as(claimantSolicitor)
            .submitExpectingErrors();

        assertThat(rejected.errors())
            .containsExactly("We could not get the fee for this application. Try submitting it again later");
        assertThat(storedEnforcements()).isEmpty();
    }

    @Test
    @DisplayName("the testing-support fee-paid shortcut pays the service request through the callback handler")
    void feePaidShortcutWithAServiceRequest() throws Exception {
        submit(SelectEnforcementType.WARRANT);
        EnforcementOrderEntity enforcement = storedEnforcement();
        String serviceRequest = raiseServiceRequest(takeScheduledServiceRequest(enforcement.getId()));

        assertThat(enforcementFeePaidSimulator.recordFeePaid(caseReference)).isTrue();

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_REVIEW);
        assertThat(feePaymentRepository.findByServiceRequestReference(serviceRequest).orElseThrow()
                       .getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(sender).send(caseReference, EnforcementEmail.WARRANT_APPLICATION_RECEIVED, Map.of());
    }

    @Test
    @DisplayName("the testing-support fee-paid shortcut still pays when no service request was raised")
    void feePaidShortcutWithoutAServiceRequest() {
        submit(SelectEnforcementType.WARRANT);
        EnforcementOrderEntity enforcement = storedEnforcement();
        schedulerClient.cancel(TaskInstanceId.of(FEES_AND_PAY_TASK_DESCRIPTOR.getTaskName(),
                                                 EnforcementFeeService.instanceId(enforcement.getId())));

        assertThat(enforcementFeePaidSimulator.recordFeePaid(caseReference)).isTrue();

        assertThat(storedEnforcement().getStatus()).isEqualTo(EnforcementStatus.AWAITING_REVIEW);
        assertThat(feePaymentRepository.findByRelatedEntityId(enforcement.getId())).isEmpty();
        verify(sender).send(caseReference, EnforcementEmail.WARRANT_APPLICATION_RECEIVED, Map.of());
        assertThat(enforcementFeePaidSimulator.recordFeePaid(caseReference)).isFalse();
    }

    private CcdEventTestSupport<PCSCase, State>.Accepted submit(SelectEnforcementType enforcementType) {
        var accepted = events.event(caseReference, EventId.enforceTheOrder.name(), application(enforcementType))
            .as(claimantSolicitor)
            .submitExpectingSuccess();
        enforcementIds.add(storedEnforcement().getId());
        return accepted;
    }

    /**
     * Reads the fees-and-pay task the submit scheduled and cancels it, so the test can do its work instead.
     */
    private FeesAndPayTaskData takeScheduledServiceRequest(UUID enforcementId) throws IOException {
        TaskInstanceId taskInstance = TaskInstanceId.of(FEES_AND_PAY_TASK_DESCRIPTOR.getTaskName(),
                                                        EnforcementFeeService.instanceId(enforcementId));
        // The client does not know the task's data class, so it hands back the serialised JSON.
        byte[] data = (byte[]) schedulerClient.getScheduledExecution(taskInstance)
            .orElseThrow(() -> new AssertionError("No service request was scheduled for " + enforcementId))
            .getData();
        schedulerClient.cancel(taskInstance);
        return objectMapper.readValue(data, FeesAndPayTaskData.class);
    }

    /**
     * What the fees-and-pay task does, against the mocked Payments.
     */
    private String raiseServiceRequest(FeesAndPayTaskData taskData) {
        String serviceRequest = "2026-" + UUID.randomUUID();
        when(paymentsClient.createServiceRequest(anyString(), any(CreateServiceRequestDTO.class)))
            .thenReturn(PaymentServiceResponse.builder().serviceRequestReference(serviceRequest).build());
        paymentService.createServiceRequest(taskData);
        return serviceRequest;
    }

    private void paymentCallback(String serviceRequest, PaymentStatus status) throws Exception {
        mockMvc.perform(put(PAYMENT_UPDATE_PATH)
                            .header(SERVICE_AUTHORIZATION, CcdEventTestSupport.SERVICE_AUTHORISATION)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "service_request_reference": "%s",
                                  "ccd_case_number": "%d",
                                  "service_request_amount": 148,
                                  "service_request_status": "%s",
                                  "payment": {
                                    "payment_amount": 148,
                                    "payment_reference": "RC-1692-2665-9206-3000",
                                    "payment_method": "payment by account",
                                    "account_number": "PBA0088311"
                                  }
                                }
                                """.formatted(serviceRequest, caseReference, status.getValue())))
            .andExpect(status().isNoContent());
    }

    private EnforcementOrderEntity storedEnforcement() {
        return storedEnforcements().getFirst();
    }

    private List<EnforcementOrderEntity> storedEnforcements() {
        return new TransactionTemplate(transactionManager).execute(ignored ->
            enforcementOrderRepository.findByClaimId(
                pcsCaseRepository.findByCaseReference(caseReference).orElseThrow().getClaims().getFirst().getId()));
    }

    private UUID claimantPartyId() {
        return new TransactionTemplate(transactionManager).execute(ignored ->
            pcsCaseRepository.findByCaseReference(caseReference).orElseThrow().getClaims().getFirst()
                .getClaimParties().stream()
                .filter(claimParty -> claimParty.getRole() == PartyRole.CLAIMANT)
                .findFirst()
                .orElseThrow()
                .getParty()
                .getId());
    }

    private List<String> scheduledDocuments(UUID enforcementId) {
        return jdbcTemplate.queryForList(
            "SELECT task_instance FROM scheduled_tasks WHERE task_name = ? AND task_instance LIKE ?",
            String.class, DOCUMENT_TASK_NAME, enforcementId + "-%");
    }

    private static PCSCase application(SelectEnforcementType enforcementType) {
        return PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder()
                .chooseEnforcementType(DynamicStringList.builder()
                    .value(DynamicStringListElement.builder().code(enforcementType.name()).build())
                    .listItems(List.of(DynamicStringListElement.builder().code(enforcementType.name()).build()))
                    .build())
                .warrantDetails(WarrantDetails.builder().statementOfTruth(statementOfTruth()).build())
                .writDetails(WritDetails.builder().statementOfTruth(statementOfTruth()).build())
                .build())
            .build();
    }

    private static StatementOfTruthDetailsEnforcement statementOfTruth() {
        StatementOfTruthDetailsEnforcement statementOfTruth = new StatementOfTruthDetailsEnforcement();
        statementOfTruth.setCompletedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE);
        statementOfTruth.setFullNameLegalRep("Clare Solicitor");
        statementOfTruth.setFirmNameLegalRep(IssuedCases.CLAIMANT);
        statementOfTruth.setPositionLegalRep("Solicitor");
        return statementOfTruth;
    }
}
