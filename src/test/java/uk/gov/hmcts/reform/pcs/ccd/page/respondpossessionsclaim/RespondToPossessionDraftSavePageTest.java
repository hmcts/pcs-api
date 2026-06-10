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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RespondToClaimSection;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.LegalRepresentativeRetriever;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.util.SelectedPartyRetriever;
import uk.gov.hmcts.reform.pcs.idam.UserInfo;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationDetailsService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class RespondToPossessionDraftSavePageTest extends BasePageTest {

    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private UserInfo userInfo;
    @Mock
    private SelectedPartyRetriever selectedPartyRetriever;
    @Mock
    private OrganisationDetailsService organisationDetailsService;
    @Mock
    private LegalRepresentativeRetriever legalRepresentativeRetriever;
    @Captor
    private ArgumentCaptor<PCSCase> pcsCaseCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextService.getCurrentUserDetails()).thenReturn(userInfo);
        lenient().when(userInfo.getRoles()).thenReturn(List.of(UserRole.CITIZEN.getRole()));
        setPageUnderTest(new RespondToPossessionDraftSavePage(
            draftCaseDataService,
            securityContextService,
            selectedPartyRetriever,
            organisationDetailsService,
            legalRepresentativeRetriever

        ));
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

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();

        verify(draftCaseDataService).saveUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );

        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails())
            .isEqualTo(contactDetails);
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses())
            .isEqualTo(responses);
    }

    @Test
    void shouldSavePartyDataIncludingImmutableFields() {
        //Given
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                       .firstName("Jack")
                       .lastName("Smith")
                       .nameKnown(VerticalYesNo.YES)
                       .addressKnown(VerticalYesNo.YES)
                       .addressSameAsProperty(VerticalYesNo.NO)
                       .build())
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .build());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).saveUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldSkipValidationWhenPartyIsNull() {
        //Given
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(null)
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeConfirmation(YesNoNotSure.YES)
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
        verify(draftCaseDataService).saveUnsubmittedEventData(
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

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).saveUnsubmittedEventData(
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
            .tenancyTypeConfirmation(YesNoNotSure.YES)
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .contactByEmail(VerticalYesNo.YES)
            .contactByPhone(VerticalYesNo.NO)
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder().defendantResponses(responses).build());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).saveUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        DefendantResponses savedResponses = savedDraft.getPossessionClaimResponse().getDefendantResponses();
        assertThat(savedResponses.getTenancyTypeConfirmation()).isEqualTo(YesNoNotSure.YES);
        assertThat(savedResponses.getRentArrearsAmountConfirmation()).isEqualTo(YesNoNotSure.NO);
        assertThat(savedResponses.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.YES);
        assertThat(savedResponses.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
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
            .anyPaymentsMade(VerticalYesNo.YES)
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .contactByText(VerticalYesNo.NO)
            .reasonableAdjustments(reasonableAdjustments)
            .householdCircumstances(householdCircumstances)
            .paymentAgreement(paymentAgreement)
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .defendantResponses(responses)
                                             .build());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).saveUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        Party savedParty = savedDraft.getPossessionClaimResponse().getDefendantContactDetails().getParty();
        DefendantResponses savedResponses = savedDraft.getPossessionClaimResponse().getDefendantResponses();

        assertThat(savedParty.getEmailAddress()).isEqualTo("jane.smith@example.com");
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(savedParty.getAddress().getAddressLine1()).isEqualTo("456 Another Road");

        assertThat(savedResponses.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
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
        verify(draftCaseDataService).saveUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails().getParty()).isNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses()).isNull();
    }

    @Test
    void shouldSaveWhenDefendantContactDetailsIsNull() {
        //Given
        DefendantResponses responses = DefendantResponses.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.NO)
            .contactByPost(VerticalYesNo.YES)
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder().defendantResponses(responses).build());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).saveUnsubmittedEventData(
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
        verify(draftCaseDataService).saveUnsubmittedEventData(
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
    void shouldRoundTripCompletedSectionsInDraft() {
        //Given
        DefendantResponses responses = DefendantResponses.builder()
            .completedSections(java.util.List.of(
                RespondToClaimSection.START_NOW_AND_DETAILS,
                RespondToClaimSection.PERSONAL_DETAILS))
            .build();

        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantResponses(responses)
                                             .build());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).saveUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim)
        );
        PCSCase savedDraft = pcsCaseCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses().getCompletedSections())
            .containsExactly(
                RespondToClaimSection.START_NOW_AND_DETAILS,
                RespondToClaimSection.PERSONAL_DETAILS);
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

        doThrow(new RuntimeException("DB connection failed"))
            .when(draftCaseDataService)
            .saveUnsubmittedEventData(anyLong(), any(), any());

        //When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        //Then
        assertThat(response.getErrors()).containsExactly(
            "We couldn't save your response. Please try again or contact support."
        );
        assertThat(response.getData()).isNull();
    }

    @Test
    void shouldSaveDraftByPartyForLegalRepresentative() {
        UUID representedPartyId = UUID.randomUUID();
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder().firstName("Jack").lastName("Smith").build())
            .build();
        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .build());
        UUID userId = UUID.randomUUID();
        String organisationId = "org";
        UUID legalRepresentativeOrg = UUID.randomUUID();
        when(securityContextService.getCurrentUserId()).thenReturn(userId);
        when(organisationDetailsService.getOrganisationIdentifier(userId.toString())).thenReturn(organisationId);
        when(selectedPartyRetriever.getSelectedPartyId(TEST_CASE_REFERENCE, organisationId))
            .thenReturn(Optional.of(representedPartyId));
        when(legalRepresentativeRetriever.getLegalRepOrganisationIdForUser(TEST_CASE_REFERENCE, organisationId))
            .thenReturn(legalRepresentativeOrg);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        verify(draftCaseDataService).saveUnsubmittedEventData(
            eq(TEST_CASE_REFERENCE), pcsCaseCaptor.capture(), eq(respondPossessionClaim), eq(representedPartyId),
            eq(legalRepresentativeOrg)
        );
    }

    @Test
    void shouldThrowErrorWhenNoSelectedPartyId() {
        when(userInfo.getRoles()).thenReturn(List.of(UserRole.DEFENDANT_SOLICITOR.getRole()));
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder().firstName("Jack").lastName("Smith").build())
            .build();
        PCSCase caseData = buildCaseData(PossessionClaimResponse.builder()
                                             .defendantContactDetails(contactDetails)
                                             .build());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).containsExactly(
            "No selected responding party id for respond to claim"
        );
        assertThat(response.getData()).isNull();
    }

    private PCSCase buildCaseData(PossessionClaimResponse possessionClaimResponse) {
        return PCSCase.builder()
            .possessionClaimResponse(possessionClaimResponse)
            .build();

    }
}

