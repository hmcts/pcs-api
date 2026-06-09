package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimState;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimFeeCalculator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.CounterClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeesAndPayTaskData;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeeService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.PaymentService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;
import uk.gov.hmcts.reform.payments.response.PaymentServiceResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType.COUNTER_CLAIM_ISSUE;

@ExtendWith(MockitoExtension.class)
class SubmitEventHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PARTY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COUNTER_CLAIM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String SERVICE_REQUEST_REFERENCE = "SR-5806-TEST";
    private static final BigDecimal FEE_AMOUNT = new BigDecimal("404.00");

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;
    @Mock
    private ClaimResponseService claimResponseService;
    @Mock
    private DefendantResponseService defendantResponseService;
    @Mock
    private CounterClaimService counterClaimService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private PartyService partyService;
    @Mock
    private FeeService feeService;
    @Mock
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SubmitEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new SubmitEventHandler(
            draftCaseDataService,
            claimResponseService,
            defendantResponseService,
            counterClaimService,
            securityContextService,
            partyService,
            feeService,
            paymentService,
            new CounterClaimFeeCalculator(),
            objectMapper
        );
    }

    @Test
    void shouldReturnErrorWhenPossessionClaimResponseIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(null)
            .build();

        stubDraft(caseData);

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst()).isEqualTo("Invalid submission: missing response data");

        verify(draftCaseDataService).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
        verify(claimResponseService, never()).saveDraftData(any(), anyLong());
        verify(defendantResponseService, never()).saveDefendantResponse(anyLong(), any());
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(anyLong(), eq(respondPossessionClaim));
    }

    @Test
    void shouldReturnErrorWhenDefendantResponsesIsNull() {
        // Given
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(null)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

        stubDraft(caseData);

        EventPayload<PCSCase, State> payload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(payload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().getFirst())
            .isEqualTo("Invalid submission: missing defendant response data");

        verify(draftCaseDataService).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
        verify(claimResponseService, never()).saveDraftData(any(), anyLong());
        verify(defendantResponseService, never()).saveDefendantResponse(anyLong(), any());
        verify(draftCaseDataService, never()).deleteUnsubmittedCaseData(anyLong(), eq(respondPossessionClaim));
    }

    @Test
    void shouldAllowSubmitWithOnlyDefendantResponses() {
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .build();

        PCSCase caseData = createDraftSaveCaseData(null, responses);

        stubDraft(caseData);
        SubmitResponse<State> result = underTest.submit(createEventPayload(caseData));

        assertThat(result.getErrors()).isNullOrEmpty();
        verify(claimResponseService).saveDraftData(caseData.getPossessionClaimResponse(), CASE_REFERENCE);
        verify(defendantResponseService).saveDefendantResponse(CASE_REFERENCE, caseData.getPossessionClaimResponse());
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldSaveAllContactPreferenceFieldsWhenFinalSubmit() {
        // Given - All contact preference fields populated
        AddressUK address = AddressUK.builder().addressLine1("123 Test Street").build();

        Party party = Party.builder()
            .firstName("Jane")
            .lastName("Smith")
            .phoneNumber("07987654321")
            .emailAddress("jane.smith@example.com")
            .address(address)
            .build();

        DefendantContactDetails defendantContactDetails =
            DefendantContactDetails.builder()
                .party(party)
                .build();

        DefendantResponses defendantResponses =
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .contactByText(VerticalYesNo.YES)
                .contactByPhone(VerticalYesNo.YES)
                .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(defendantContactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        stubDraft(caseData);

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        // Verify the exact response object is passed to the service
        ArgumentCaptor<PossessionClaimResponse> responseCaptor =
            ArgumentCaptor.forClass(PossessionClaimResponse.class);
        verify(claimResponseService).saveDraftData(responseCaptor.capture(), eq(CASE_REFERENCE));

        PossessionClaimResponse capturedResponse = responseCaptor.getValue();
        assertThat(capturedResponse.getDefendantResponses().getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(capturedResponse.getDefendantResponses().getContactByText()).isEqualTo(VerticalYesNo.YES);
        assertThat(capturedResponse.getDefendantResponses().getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(capturedResponse.getDefendantContactDetails().getParty().getPhoneNumber())
            .isEqualTo("07987654321");
        assertThat(capturedResponse.getDefendantContactDetails().getParty().getEmailAddress())
            .isEqualTo("jane.smith@example.com");
        assertThat(capturedResponse.getDefendantContactDetails().getParty().getAddress().getAddressLine1())
            .isEqualTo("123 Test Street");
    }

    @Test
    void shouldHandleContactPreferencesWithNullValues() {
        // Given - Some null preferences

        DefendantResponses defendantResponses =
            DefendantResponses.builder()
                .contactByText(VerticalYesNo.YES)
                .contactByPhone(VerticalYesNo.YES)
                .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();
        stubDraft(caseData);

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        // Verify service handles null values gracefully
        verify(claimResponseService).saveDraftData(response, CASE_REFERENCE);
    }

    @Test
    void shouldHandleExceptionFromContactPreferencesService() {
        // Given
        Party party = Party.builder()
            .firstName("Test")
            .lastName("User")
            .build();

        DefendantContactDetails defendantContactDetails = DefendantContactDetails.builder()
            .party(party).build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByPhone(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(defendantContactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();
        stubDraft(caseData);

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        //when

        // Mock service to throw exception
        doThrow(new IllegalStateException("No party found for IDAM ID"))
            .when(claimResponseService).saveDraftData(any(), anyLong());

        // When / Then - Exception should propagate (not caught by handler)
        // This tests that the handler doesn't swallow exceptions
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> underTest.submit(eventPayload)
        )).hasMessage("No party found for IDAM ID");

        verify(claimResponseService).saveDraftData(response, CASE_REFERENCE);
    }

    @Test
    void shouldSubmitRegularIncomeFieldsWhenFinalSubmit() {
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .shareIncomeExpenseDetails(YesOrNo.YES)
            .incomeFromJobs(YesOrNo.YES)
            .incomeFromJobsAmount(new BigDecimal("200000")) // £2000.00 in pence
            .incomeFromJobsFrequency(RecurrenceFrequency.MONTHLY)
            .pension(YesOrNo.NO)
            .universalCredit(YesOrNo.YES)
            .ucApplicationDate(LocalDate.of(2024, 2, 10))
            .universalCreditAmount(new BigDecimal("100000")) // £1000.00 in pence
            .universalCreditFrequency(RecurrenceFrequency.MONTHLY)
            .otherBenefits(YesOrNo.NO)
            .moneyFromElsewhere(YesOrNo.YES)
            .moneyFromElsewhereDetails("Receive child support payments")
            .build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .householdCircumstances(householdCircumstances)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        stubDraft(caseData);

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();

        // Verify the response object with household circumstances is passed to the service
        ArgumentCaptor<PossessionClaimResponse> responseCaptor =
            ArgumentCaptor.forClass(PossessionClaimResponse.class);
        verify(claimResponseService).saveDraftData(responseCaptor.capture(), eq(CASE_REFERENCE));

        PossessionClaimResponse capturedResponse = responseCaptor.getValue();
        HouseholdCircumstances capturedHousehold = capturedResponse.getDefendantResponses()
            .getHouseholdCircumstances();

        // Assert all regular income fields are submitted correctly
        assertThat(capturedHousehold.getShareIncomeExpenseDetails()).isEqualTo(YesOrNo.YES);

        assertThat(capturedHousehold.getIncomeFromJobs()).isEqualTo(YesOrNo.YES);
        assertThat(capturedHousehold.getIncomeFromJobsAmount()).isEqualByComparingTo("200000");
        assertThat(capturedHousehold.getIncomeFromJobsFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);

        assertThat(capturedHousehold.getPension()).isEqualTo(YesOrNo.NO);

        assertThat(capturedHousehold.getUniversalCredit()).isEqualTo(YesOrNo.YES);
        assertThat(capturedHousehold.getUcApplicationDate()).isEqualTo(LocalDate.of(2024, 2, 10));
        assertThat(capturedHousehold.getUniversalCreditAmount()).isEqualByComparingTo("100000");
        assertThat(capturedHousehold.getUniversalCreditFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);

        assertThat(capturedHousehold.getOtherBenefits()).isEqualTo(YesOrNo.NO);

        assertThat(capturedHousehold.getMoneyFromElsewhere()).isEqualTo(YesOrNo.YES);
        assertThat(capturedHousehold.getMoneyFromElsewhereDetails())
            .isEqualTo("Receive child support payments");

        verify(defendantResponseService).saveDefendantResponse(CASE_REFERENCE, capturedResponse);
        verify(draftCaseDataService).deleteUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldCreateServiceRequestAndReturnConfirmationBodyWhenCounterClaimPaymentRequired() throws Exception {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("6000"))
            .build();
        PCSCase caseData = createCounterClaimPaymentDraft(counterClaim);
        stubDraft(caseData);
        stubCounterClaimPaymentDependencies();

        FeeDetails feeDetails = FeeDetails.builder()
            .code("FEE0441")
            .description("Counterclaim fee")
            .feeAmount(FEE_AMOUNT)
            .version(1)
            .build();
        when(feeService.getFee(FeeType.COUNTER_CLAIM)).thenReturn(feeDetails);
        when(paymentService.createServiceRequest(any(FeesAndPayTaskData.class)))
            .thenReturn(PaymentServiceResponse.builder().serviceRequestReference(SERVICE_REQUEST_REFERENCE).build());

        SubmitResponse<State> result = underTest.submit(createEventPayload(caseData));

        assertThat(result.getErrors()).isNullOrEmpty();
        assertThat(result.getConfirmationBody()).isNotBlank();

        JsonNode confirmation = objectMapper.readTree(result.getConfirmationBody());
        JsonNode counterClaimNode = confirmation.get("counterClaim");
        assertThat(counterClaimNode.get("status").asText())
            .isEqualTo(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED.name());
        assertThat(counterClaimNode.get("serviceRequestReference").asText()).isEqualTo(SERVICE_REQUEST_REFERENCE);
        assertThat(counterClaimNode.get("feeAmount").decimalValue()).isEqualByComparingTo(FEE_AMOUNT);

        ArgumentCaptor<FeesAndPayTaskData> taskDataCaptor = ArgumentCaptor.forClass(FeesAndPayTaskData.class);
        verify(paymentService).createServiceRequest(taskDataCaptor.capture());
        FeesAndPayTaskData taskData = taskDataCaptor.getValue();
        assertThat(taskData.getCaseReference()).isEqualTo(CASE_REFERENCE);
        assertThat(taskData.getCcdCaseNumber()).isEqualTo(String.valueOf(CASE_REFERENCE));
        assertThat(taskData.getResponsiblePartyId()).isEqualTo(PARTY_ID);
        assertThat(taskData.getPaymentCallbackHandlerType()).isEqualTo(COUNTER_CLAIM_ISSUE);
        assertThat(taskData.getRelatedEntityId()).isEqualTo(COUNTER_CLAIM_ID);
        assertThat(taskData.getFeeDetails()).isEqualTo(feeDetails);
    }

    @Test
    void shouldReturnDefaultResponseWhenCounterClaimHasHwfReference() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .hwfReferenceNumber("HWF-123-456")
            .build();
        PCSCase caseData = createCounterClaimPaymentDraft(counterClaim);
        stubDraft(caseData);

        SubmitResponse<State> result = underTest.submit(createEventPayload(caseData));

        assertThat(result.getErrors()).isNullOrEmpty();
        assertThat(result.getConfirmationBody()).isNull();
        verify(paymentService, never()).createServiceRequest(any(FeesAndPayTaskData.class));
    }

    @Test
    void shouldReturnDefaultResponseWhenMakeCounterClaimIsNotYes() {
        DefendantResponses responses = DefendantResponses.builder()
            .makeCounterClaim(VerticalYesNo.NO)
            .counterClaim(CounterClaim.builder()
                .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
                .build())
            .build();
        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder().defendantResponses(responses).build())
            .build();
        stubDraft(caseData);

        SubmitResponse<State> result = underTest.submit(createEventPayload(caseData));

        assertThat(result.getErrors()).isNullOrEmpty();
        assertThat(result.getConfirmationBody()).isNull();
        verify(paymentService, never()).createServiceRequest(any(FeesAndPayTaskData.class));
    }

    @Test
    void shouldUseCounterClaimFlatFeeWhenClaimTypeIsSomethingElse() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.SOMETHING_ELSE)
            .build();
        PCSCase caseData = createCounterClaimPaymentDraft(counterClaim);
        stubDraft(caseData);
        stubCounterClaimPaymentDependencies();
        stubFeeLookupAndServiceRequest(FeeType.COUNTER_CLAIM_FLAT_FEE);

        underTest.submit(createEventPayload(caseData));

        verify(feeService).getFee(FeeType.COUNTER_CLAIM_FLAT_FEE);
    }

    @Test
    void shouldUseCounterClaimRangedFeeWhenKnownAmountIsFiveThousandOrLess() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("5000"))
            .build();
        PCSCase caseData = createCounterClaimPaymentDraft(counterClaim);
        stubDraft(caseData);
        stubCounterClaimPaymentDependencies();
        stubFeeLookupAndServiceRequest(FeeType.COUNTER_CLAIM_RANGED);

        underTest.submit(createEventPayload(caseData));

        verify(feeService).getFee(FeeType.COUNTER_CLAIM_RANGED);
    }

    @Test
    void shouldUseCounterClaimFeeWhenKnownAmountExceedsFiveThousand() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.YES)
            .claimAmount(new BigDecimal("5001"))
            .build();
        PCSCase caseData = createCounterClaimPaymentDraft(counterClaim);
        stubDraft(caseData);
        stubCounterClaimPaymentDependencies();
        stubFeeLookupAndServiceRequest(FeeType.COUNTER_CLAIM);

        underTest.submit(createEventPayload(caseData));

        verify(feeService).getFee(FeeType.COUNTER_CLAIM);
    }

    @Test
    void shouldUseEstimatedMaxAmountWhenClaimAmountIsUnknown() {
        CounterClaim counterClaim = CounterClaim.builder()
            .claimType(CounterClaimType.PAYMENT_OR_COMPENSATION)
            .isClaimAmountKnown(VerticalYesNo.NO)
            .estimatedMaxClaimAmount(new BigDecimal("2500"))
            .build();
        PCSCase caseData = createCounterClaimPaymentDraft(counterClaim);
        stubDraft(caseData);
        stubCounterClaimPaymentDependencies();
        stubFeeLookupAndServiceRequest(FeeType.COUNTER_CLAIM_RANGED);

        underTest.submit(createEventPayload(caseData));

        verify(feeService).getFee(FeeType.COUNTER_CLAIM_RANGED);
    }

    @Test
    void shouldCallContactPreferencesServiceBeforeReturningSuccess() {
        // Given
        Party party = Party.builder()
            .firstName("Test")
            .lastName("User")
            .build();

        DefendantContactDetails defendantContactDetails = DefendantContactDetails.builder()
            .party(party).build();

        DefendantResponses defendantResponses = DefendantResponses.builder()
            .contactByPhone(VerticalYesNo.YES)
            .build();

        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(defendantContactDetails)
            .defendantResponses(defendantResponses)
            .build();

        PCSCase caseData = PCSCase.builder()
            .possessionClaimResponse(response)
            .build();

        stubDraft(caseData);

        EventPayload<PCSCase, State> eventPayload = createEventPayload(caseData);

        // When
        SubmitResponse<State> result = underTest.submit(eventPayload);

        // Then - Verify service is called and success is returned
        verify(claimResponseService).saveDraftData(response, CASE_REFERENCE);
        assertThat(result).isNotNull();
        assertThat(result.getErrors()).isNullOrEmpty();
        assertThat(result.getState()).isNull(); // Default response has null state
    }

    private void stubDraft(PCSCase draft) {
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(draft));
    }

    private EventPayload<PCSCase, State> createEventPayload(PCSCase caseData) {
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        return eventPayload;
    }

    private PCSCase createDraftSaveCaseData(DefendantContactDetails contactDetails, DefendantResponses responses) {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(contactDetails)
            .defendantResponses(responses != null ? responses : DefendantResponses.builder().build())
            .build();

        return PCSCase.builder()
            .possessionClaimResponse(response)
            .build();
    }

    private PCSCase createCounterClaimPaymentDraft(CounterClaim counterClaim) {
        DefendantResponses responses = DefendantResponses.builder()
            .makeCounterClaim(VerticalYesNo.YES)
            .counterClaim(counterClaim)
            .build();

        return PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder().defendantResponses(responses).build())
            .build();
    }

    private void stubCounterClaimPaymentDependencies() {
        PartyEntity partyEntity = PartyEntity.builder().id(PARTY_ID).build();
        CounterClaimEntity counterClaimEntity = CounterClaimEntity.builder()
            .id(COUNTER_CLAIM_ID)
            .status(CounterClaimState.PENDING_COUNTER_CLAIM_ISSUED)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(partyService.getPartyEntityByIdamId(USER_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        when(counterClaimService.saveCounterClaim(eq(CASE_REFERENCE), any(CounterClaim.class)))
            .thenReturn(Optional.of(counterClaimEntity));
    }

    private void stubFeeLookupAndServiceRequest(FeeType feeType) {
        FeeDetails feeDetails = FeeDetails.builder()
            .code("FEE0441")
            .description("Counterclaim fee")
            .feeAmount(FEE_AMOUNT)
            .version(1)
            .build();
        when(feeService.getFee(feeType)).thenReturn(feeDetails);
        when(paymentService.createServiceRequest(any(FeesAndPayTaskData.class)))
            .thenReturn(PaymentServiceResponse.builder().serviceRequestReference(SERVICE_REQUEST_REFERENCE).build());
    }
}
