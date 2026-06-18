package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularExpenseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeItemEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.ccd.domain.IncomeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RegularExpenseType;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefendantResponseReadMapperTest {

    @Test
    void shouldMapClaimIssuedDate() {
        PartyEntity party = mock(PartyEntity.class);
        when(party.getId()).thenReturn(null);
        when(party.getAddressSameAsProperty()).thenReturn(null);
        when(party.getAddress()).thenReturn(null);
        when(party.getDateOfBirth()).thenReturn(null);
        when(party.getContactPreferences()).thenReturn(null);

        ClaimEntity claim = ClaimEntity.builder()
            .claimIssuedDate(LocalDateTime.of(2026, 2, 5, 10, 30))
            .build();
        DefendantResponseEntity entity = DefendantResponseEntity.builder()
            .party(party)
            .pcsCase(null)
            .claim(claim)
            .build();

        DefendantResponseReadMapper underTest = new DefendantResponseReadMapper(mock(AddressMapper.class));

        PossessionClaimResponse response = underTest.toPossessionClaimResponse(entity, List.of());

        assertThat(response.getClaimIssuedDate()).isEqualTo(LocalDate.of(2026, 2, 5));
    }

    @Test
    void shouldMapHouseholdIncomeExpensesPaymentAgreementAddressAndCounterClaim() {
        final AddressMapper addressMapper = mock(AddressMapper.class);

        final UUID defendantPartyId = UUID.randomUUID();
        final PartyEntity party = mock(PartyEntity.class);
        when(party.getId()).thenReturn(defendantPartyId);
        when(party.getFirstName()).thenReturn("Test");
        when(party.getLastName()).thenReturn("Defendant");
        when(party.getEmailAddress()).thenReturn("def@example.com");
        when(party.getPhoneNumber()).thenReturn("07000111222");
        when(party.getPhoneNumberProvided()).thenReturn(VerticalYesNo.YES);
        when(party.getDateOfBirth()).thenReturn(LocalDate.of(1990, 2, 3));
        when(party.getContactPreferences()).thenReturn(null);

        when(party.getAddressSameAsProperty()).thenReturn(VerticalYesNo.YES);
        final AddressEntity propertyAddress = AddressEntity.builder().postcode("AA1 1AA").build();
        final uk.gov.hmcts.ccd.sdk.type.AddressUK mappedAddress =
            uk.gov.hmcts.ccd.sdk.type.AddressUK.builder().postCode("AA1 1AA").build();
        when(addressMapper.toAddressUK(propertyAddress)).thenReturn(mappedAddress);
        final AddressEntity partyAddress = AddressEntity.builder().postcode("SHOULD_NOT_USE").build();
        when(party.getAddress()).thenReturn(partyAddress);

        final PcsCaseEntity pcsCase = PcsCaseEntity.builder().propertyAddress(propertyAddress).build();
        final DefendantResponseReadMapper underTest = new DefendantResponseReadMapper(addressMapper);

        final RegularExpenseEntity householdBills = RegularExpenseEntity.builder()
            .expenseType(RegularExpenseType.HOUSEHOLD_BILLS)
            .amount(new BigDecimal("10"))
            .expenseFrequency(RecurrenceFrequency.WEEKLY)
            .build();
        final RegularExpenseEntity clothing = RegularExpenseEntity.builder()
            .expenseType(RegularExpenseType.CLOTHING)
            .amount(new BigDecimal("20"))
            .expenseFrequency(RecurrenceFrequency.MONTHLY)
            .build();

        final RegularIncomeItemEntity jobs = RegularIncomeItemEntity.builder()
            .incomeType(IncomeType.INCOME_FROM_JOBS)
            .amount(new BigDecimal("100"))
            .frequency(RecurrenceFrequency.WEEKLY)
            .build();
        final RegularIncomeItemEntity universalCredit = RegularIncomeItemEntity.builder()
            .incomeType(IncomeType.UNIVERSAL_CREDIT)
            .amount(new BigDecimal("200"))
            .frequency(RecurrenceFrequency.MONTHLY)
            .build();
        final RegularIncomeEntity regularIncome = RegularIncomeEntity.builder().otherIncomeDetails("Friends").build();
        regularIncome.addItem(jobs);
        regularIncome.addItem(universalCredit);

        final HouseholdCircumstancesEntity hc = HouseholdCircumstancesEntity.builder()
            .dependantChildren(VerticalYesNo.YES)
            .otherDependants(VerticalYesNo.NO)
            .otherTenants(VerticalYesNo.NO)
            .exceptionalHardship(VerticalYesNo.YES)
            .priorityDebts(VerticalYesNo.YES)
            .debtTotal(new BigDecimal("999"))
            .debtContribution(new BigDecimal("50"))
            .debtContributionFrequency(
                uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency.MONTHLY
            )
            .universalCredit(VerticalYesNo.YES)
            .ucApplicationDate(LocalDate.of(2024, 1, 1))
            .regularIncomeEntity(regularIncome)
            .build();
        hc.addRegularExpense(householdBills);
        hc.addRegularExpense(clothing);

        final PaymentAgreementEntity payment = PaymentAgreementEntity.builder()
            .anyPaymentsMade(VerticalYesNo.YES)
            .paymentDetails("Paid some")
            .repaymentPlanAgreed(YesNoNotSure.YES)
            .repaymentAgreedDetails("Plan details")
            .repayArrearsInstalments(VerticalYesNo.NO)
            .additionalRentContribution(new BigDecimal("12.34"))
            .additionalContributionFrequency("WEEKLY")
            .build();

        final CounterClaimEntity counterClaim = CounterClaimEntity.builder()
            .party(party)
            .build();
        pcsCase.setCounterClaims(List.of(counterClaim));

        DefendantResponseEntity entity = DefendantResponseEntity.builder()
            .party(party)
            .pcsCase(pcsCase)
            .householdCircumstances(hc)
            .paymentAgreement(payment)
            .build();

        PossessionClaimResponse response = underTest.toPossessionClaimResponse(entity, List.of());

        assertThat(response.getDefendantContactDetails().getParty().getAddress()).isEqualTo(mappedAddress);
        assertThat(response.getDefendantResponses().getPaymentAgreement()).isNotNull();
        assertThat(response.getDefendantResponses().getHouseholdCircumstances()).isNotNull();
        assertThat(response.getDefendantResponses().getCounterClaim()).isNotNull();

        verify(addressMapper, never()).toAddressUK(partyAddress);
    }

    @Test
    void shouldMapContactPreferencesAndStatementOfTruthAndUsePartyAddressWhenNotSameAsProperty() {
        final AddressMapper addressMapper = mock(AddressMapper.class);
        final DefendantResponseReadMapper underTest = new DefendantResponseReadMapper(addressMapper);

        final PartyEntity party = mock(PartyEntity.class);
        when(party.getId()).thenReturn(UUID.randomUUID());
        when(party.getAddressSameAsProperty()).thenReturn(VerticalYesNo.NO);

        final AddressEntity partyAddress = AddressEntity.builder().postcode("ZZ1 1ZZ").build();
        final uk.gov.hmcts.ccd.sdk.type.AddressUK mappedPartyAddress =
            uk.gov.hmcts.ccd.sdk.type.AddressUK.builder().postCode("ZZ1 1ZZ").build();
        when(party.getAddress()).thenReturn(partyAddress);
        when(addressMapper.toAddressUK(partyAddress)).thenReturn(mappedPartyAddress);

        final ContactPreferencesEntity prefs = ContactPreferencesEntity.builder()
            .contactByEmail(VerticalYesNo.YES)
            .contactByPhone(VerticalYesNo.NO)
            .contactByText(VerticalYesNo.YES)
            .contactByPost(VerticalYesNo.NO)
            .build();
        when(party.getContactPreferences()).thenReturn(prefs);

        final DefendantResponseEntity entity = DefendantResponseEntity.builder()
            .party(party)
            .pcsCase(
                PcsCaseEntity.builder()
                    .propertyAddress(AddressEntity.builder().postcode("SHOULD_NOT_USE").build())
                    .build()
            )
            .statementOfTruth(StatementOfTruthEntity.builder().fullName("  Jane Doe  ").build())
            .build();

        final PossessionClaimResponse response = underTest.toPossessionClaimResponse(entity, List.of());

        assertThat(response.getDefendantContactDetails().getParty().getAddress()).isEqualTo(mappedPartyAddress);
        assertThat(response.getDefendantResponses().getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(response.getDefendantResponses().getContactByPhone()).isEqualTo(VerticalYesNo.NO);
        assertThat(response.getDefendantResponses().getContactByText()).isEqualTo(VerticalYesNo.YES);
        assertThat(response.getDefendantResponses().getContactByPost()).isEqualTo(VerticalYesNo.NO);
        assertThat(response.getDefendantResponses().getStatementOfTruthCompletedBy()).isEqualTo("  Jane Doe  ");
    }

    @Test
    void shouldMarkUniversalCreditYesWhenNoUcApplicationDateAndNoUcIncomeItem() {
        final AddressMapper addressMapper = mock(AddressMapper.class);
        final DefendantResponseReadMapper underTest = new DefendantResponseReadMapper(addressMapper);

        final PartyEntity party = mock(PartyEntity.class);
        when(party.getId()).thenReturn(UUID.randomUUID());
        when(party.getAddressSameAsProperty()).thenReturn(null);
        when(party.getAddress()).thenReturn(null);
        when(party.getContactPreferences()).thenReturn(null);
        when(party.getDateOfBirth()).thenReturn(null);

        final RegularIncomeEntity regularIncome = RegularIncomeEntity.builder().otherIncomeDetails(null).build();
        regularIncome.addItem(
            RegularIncomeItemEntity.builder()
                .incomeType(IncomeType.PENSION)
                .amount(new BigDecimal("1"))
                .frequency(RecurrenceFrequency.MONTHLY)
                .build()
        );

        final HouseholdCircumstancesEntity hc = HouseholdCircumstancesEntity.builder()
            .universalCredit(VerticalYesNo.YES)
            .ucApplicationDate(null)
            .regularIncomeEntity(regularIncome)
            .build();

        final DefendantResponseEntity entity = DefendantResponseEntity.builder()
            .party(party)
            .pcsCase(null)
            .householdCircumstances(hc)
            .build();

        final PossessionClaimResponse response = underTest.toPossessionClaimResponse(entity, List.of());
        assertThat(response.getDefendantResponses().getHouseholdCircumstances().getUniversalCredit())
            .isEqualTo(YesOrNo.YES);
        assertThat(response.getDefendantResponses().getHouseholdCircumstances().getHasAppliedForUniversalCredit())
            .isNull();
    }

    @Test
    void shouldMapPartyAssertionsToDefendantResponses() {
        final PartyEntity party = mock(PartyEntity.class);
        when(party.getId()).thenReturn(UUID.randomUUID());
        when(party.getAddressSameAsProperty()).thenReturn(null);
        when(party.getAddress()).thenReturn(null);
        when(party.getContactPreferences()).thenReturn(null);
        when(party.getDateOfBirth()).thenReturn(null);

        final DefendantResponseEntity entity = DefendantResponseEntity.builder()
            .party(party)
            .pcsCase(null)
            .build();

        final List<PartyAttributeAssertationEntity> assertions = List.of(
            PartyAttributeAssertationEntity.builder()
                .attributesName(PartyAttributeType.TENANCY_TYPE)
                .assertedValue("Corrected tenancy type")
                .build(),
            PartyAttributeAssertationEntity.builder()
                .attributesName(PartyAttributeType.TENANCY_START_DATE)
                .assertedValue("2020-01-15")
                .build(),
            PartyAttributeAssertationEntity.builder()
                .attributesName(PartyAttributeType.RENT_ARREARS_AMOUNT)
                .assertedValue("500.00")
                .build()
        );

        final PossessionClaimResponse response = new DefendantResponseReadMapper(mock(AddressMapper.class))
            .toPossessionClaimResponse(entity, assertions);

        assertThat(response.getDefendantResponses().getTenancyType()).isEqualTo("Corrected tenancy type");
        assertThat(response.getDefendantResponses().getTenancyStartDate()).isEqualTo(LocalDate.of(2020, 1, 15));
        assertThat(response.getDefendantResponses().getRentArrearsAmount()).isEqualByComparingTo("500.00");
    }
}

