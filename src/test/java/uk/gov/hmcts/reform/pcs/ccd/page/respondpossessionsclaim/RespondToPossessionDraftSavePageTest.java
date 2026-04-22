package uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionsclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.ContactPreferenceType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PaymentAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ReasonableAdjustments;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class RespondToPossessionDraftSavePageTest extends BasePageTest {

    @Mock
    private ImmutablePartyFieldValidator immutableFieldValidator;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Captor
    private ArgumentCaptor<PCSCase> pcsCaseCaptor;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RespondToPossessionDraftSavePage(immutableFieldValidator, draftCaseDataService));
    }

    @Test
    void shouldReturnPartialUpdate() {
        //Given
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder().firstName("Jack").lastName("Smith").build())
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .possessionNoticeReceived(YesNoNotSure.NO)
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .defendantResponses(responses)
                                             .build());

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();

        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails())
            .isEqualTo(contactDetails);
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses())
            .isEqualTo(responses);
    }

    @Test
    void shouldReturnErrorsWhenImmutableFieldViolationsFound() {
        //Given
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                       .nameKnown(VerticalYesNo.YES)
                       .addressKnown(VerticalYesNo.YES)
                       .addressSameAsProperty(VerticalYesNo.NO)
                       .build())
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .build());

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of("nameKnown", "addressKnown", "addressSameAsProperty"));

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).containsExactly(
            "Invalid submission: immutable field must not be sent: nameKnown",
            "Invalid submission: immutable field must not be sent: addressKnown",
            "Invalid submission: immutable field must not be sent: addressSameAsProperty"
        );
        assertThat(response.getData()).isNull();
        verify(draftCaseDataService, never()).patchUnsubmittedEventData(anyLong(), any(), any());
    }

    @Test
    void shouldSkipValidationWhenPartyIsNull() {
        //Given
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(null)
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .defendantResponses(responses)
                                             .build());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(immutableFieldValidator);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails().getParty()).isNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses()).isEqualTo(responses);
    }

    @Test
    void shouldSaveCompletePartyData() {
        //Given
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                       .firstName("John")
                       .lastName("Doe")
                       .emailAddress("john.doe@example.com")
                       .phoneNumber("07700900000")
                       .address(address)
                       .build())
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .build());

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        Party savedParty = savedDraft.getPossessionClaimResponse().getDefendantContactDetails().getParty();
        assertThat(savedParty.getFirstName()).isEqualTo("John");
        assertThat(savedParty.getLastName()).isEqualTo("Doe");
        assertThat(savedParty.getEmailAddress()).isEqualTo("john.doe@example.com");
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07700900000");
        assertThat(savedParty.getAddress().getAddressLine1()).isEqualTo("123 Test Street");
        assertThat(savedParty.getAddress().getPostTown()).isEqualTo("London");
        assertThat(savedParty.getAddress().getPostCode()).isEqualTo("SW1A 1AA");
    }

    @Test
    void shouldSaveDefendantResponsesData() {
        //Given
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .preferenceType(ContactPreferenceType.EMAIL)
            .contactByPhone(VerticalYesNo.NO)
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder().defendantResponses(responses).build());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(immutableFieldValidator);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        DefendantResponses savedResponses = savedDraft.getPossessionClaimResponse().getDefendantResponses();
        assertThat(savedResponses.getTenancyTypeCorrect()).isEqualTo(YesNoNotSure.YES);
        assertThat(savedResponses.getRentArrearsAmountConfirmation()).isEqualTo(YesNoNotSure.NO);
        assertThat(savedResponses.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.YES);
        assertThat(savedResponses.getPreferenceType()).isEqualTo(ContactPreferenceType.EMAIL);
        assertThat(savedResponses.getContactByPhone()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldSaveAllDefendantSectionsInPartialUpdate() {
        //Given
        AddressUK address = AddressUK.builder()
            .addressLine1("456 Another Road")
            .postCode("M1 1AA")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                       .emailAddress("jane.smith@example.com")
                       .phoneNumber("07123456789")
                       .address(address)
                       .build())
            .build();

        ReasonableAdjustments reasonableAdjustments = ReasonableAdjustments.builder()
            .reasonableAdjustmentsRequired("Wheelchair access")
            .build();

        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .dependantChildren(YesOrNo.YES)
            .build();

        PaymentAgreement paymentAgreement = PaymentAgreement.builder()
            .anyPaymentsMade(uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES)
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .preferenceType(ContactPreferenceType.EMAIL)
            .contactByText(VerticalYesNo.NO)
            .reasonableAdjustments(reasonableAdjustments)
            .householdCircumstances(householdCircumstances)
            .paymentAgreement(paymentAgreement)
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .defendantResponses(responses)
                                             .build());

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        Party savedParty = savedDraft.getPossessionClaimResponse().getDefendantContactDetails().getParty();
        DefendantResponses savedResponses = savedDraft.getPossessionClaimResponse().getDefendantResponses();

        assertThat(savedParty.getEmailAddress()).isEqualTo("jane.smith@example.com");
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(savedParty.getAddress().getAddressLine1()).isEqualTo("456 Another Road");

        assertThat(savedResponses.getPreferenceType()).isEqualTo(ContactPreferenceType.EMAIL);
        assertThat(savedResponses.getContactByText()).isEqualTo(VerticalYesNo.NO);

        DefendantResponses savedResponse = savedDraft.getPossessionClaimResponse().getDefendantResponses();
        assertThat(savedResponse.getReasonableAdjustments()).isEqualTo(reasonableAdjustments);
        assertThat(savedResponse.getHouseholdCircumstances()).isEqualTo(householdCircumstances);
        assertThat(savedResponse.getPaymentAgreement()).isEqualTo(paymentAgreement);
    }

    @Test
    void shouldAllowNullPartyInPartialUpdate() {
        //Given
        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(
                                                 DefendantContactDetails.builder()
                                                     .party(null)
                                                     .build())
                                             .build());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(immutableFieldValidator);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails().getParty()).isNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses()).isNull();
    }

    @Test
    void shouldSkipValidationWhenDefendantContactDetailsIsNull() {
        //Given
        DefendantResponses responses = DefendantResponses.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.NO)
            .preferenceType(ContactPreferenceType.POST)
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder().defendantResponses(responses).build());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(immutableFieldValidator);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails()).isNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses()).isEqualTo(responses);
    }

    @Test
    void shouldSaveRegularIncomeFieldsInDraft() {
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .shareIncomeExpenseDetails(YesOrNo.YES)
            .incomeFromJobs(YesOrNo.YES)
            .incomeFromJobsAmount(new BigDecimal("150000")) // £1500.00 in pence
            .incomeFromJobsFrequency(RecurrenceFrequency.MONTHLY)
            .pension(YesOrNo.YES)
            .pensionAmount(new BigDecimal("50000")) // £500.00 in pence
            .pensionFrequency(RecurrenceFrequency.MONTHLY)
            .universalCredit(YesOrNo.YES)
            .ucApplicationDate(LocalDate.of(2024, 1, 15))
            .universalCreditAmount(new BigDecimal("80000")) // £800.00 in pence
            .universalCreditFrequency(RecurrenceFrequency.MONTHLY)
            .otherBenefits(YesOrNo.YES)
            .otherBenefitsAmount(new BigDecimal("20000")) // £200.00 in pence
            .otherBenefitsFrequency(RecurrenceFrequency.WEEKLY)
            .moneyFromElsewhere(YesOrNo.YES)
            .moneyFromElsewhereDetails("Child maintenance payments of £100 per week")
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .householdCircumstances(householdCircumstances)
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantResponses(responses)
                                             .build());

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        // Then
        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        HouseholdCircumstances savedHousehold = savedDraft.getPossessionClaimResponse()
            .getDefendantResponses()
            .getHouseholdCircumstances();

        // Assert all regular income fields are saved correctly
        assertThat(savedHousehold.getShareIncomeExpenseDetails()).isEqualTo(YesOrNo.YES);

        assertThat(savedHousehold.getIncomeFromJobs()).isEqualTo(YesOrNo.YES);
        assertThat(savedHousehold.getIncomeFromJobsAmount()).isEqualByComparingTo("150000");
        assertThat(savedHousehold.getIncomeFromJobsFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);

        assertThat(savedHousehold.getPension()).isEqualTo(YesOrNo.YES);
        assertThat(savedHousehold.getPensionAmount()).isEqualByComparingTo("50000");
        assertThat(savedHousehold.getPensionFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);

        assertThat(savedHousehold.getUniversalCredit()).isEqualTo(YesOrNo.YES);
        assertThat(savedHousehold.getUcApplicationDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(savedHousehold.getUniversalCreditAmount()).isEqualByComparingTo("80000");
        assertThat(savedHousehold.getUniversalCreditFrequency()).isEqualTo(RecurrenceFrequency.MONTHLY);

        assertThat(savedHousehold.getOtherBenefits()).isEqualTo(YesOrNo.YES);
        assertThat(savedHousehold.getOtherBenefitsAmount()).isEqualByComparingTo("20000");
        assertThat(savedHousehold.getOtherBenefitsFrequency()).isEqualTo(RecurrenceFrequency.WEEKLY);

        assertThat(savedHousehold.getMoneyFromElsewhere()).isEqualTo(YesOrNo.YES);
        assertThat(savedHousehold.getMoneyFromElsewhereDetails())
            .isEqualTo("Child maintenance payments of £100 per week");
    }

    @Test
    void shouldReturnErrorWhenDraftSaveFails() {
        //Given
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder().firstName("Jack").lastName("Smith").build())
            .build();


        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .build());

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of());

        doThrow(new RuntimeException("DB connection failed"))
            .when(draftCaseDataService)
            .patchUnsubmittedEventData(anyLong(), any(), any());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).containsExactly(
            "We couldn't save your response. Please try again or contact support."
        );
        assertThat(response.getData()).isNull();
    }

    private PCSCase buildCaseData(PossessionClaimResponse possessionClaimResponse) {
        return PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

    }
}

