package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.IncomeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertionStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RecurrenceFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.RegularExpenseType;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularExpenseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeItemEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAttributeAssertionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormAmountRow;
import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormPayload;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DefenceFormPayloadBuilderTest {

    private static final UUID DEFENDANT_ID = UUID.randomUUID();

    // Fixed UK clock: generation happens on 16 July 2026 (BST).
    private static final Clock UK_CLOCK =
        Clock.fixed(Instant.parse("2026-07-16T08:00:00Z"), ZoneId.of("Europe/London"));

    @Mock
    private PartyAttributeAssertionRepository assertionRepository;

    private DefenceFormPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DefenceFormPayloadBuilder(
            new CaseReferenceFormatter(),
            new CaseNameFormatter(),
            assertionRepository,
            new ObjectMapper(),
            UK_CLOCK);
        stubAssertions();
    }

    @Nested
    class HeaderAndParties {

        @Test
        void mapsReferenceCountryAndCaseName() {
            DefenceFormPayload payload = builder.build(response(LegislativeCountry.ENGLAND));

            assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-1234-5678");
            assertThat(payload.isEngland()).isTrue();
            assertThat(payload.isWales()).isFalse();
            assertThat(payload.getCaseName()).isEqualTo("Alice Owner vs Bob Tenant");
        }

        @Test
        void mapsIssueDateFromClaim() {
            DefenceFormPayload payload = builder.build(response(LegislativeCountry.ENGLAND));
            assertThat(payload.getIssueDateSealed()).isEqualTo(LocalDate.of(2026, 1, 5));
        }

        @Test
        void issueDateUsesUkCalendarDayForTimestampJustAfterMidnightBst() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            // 23:30 UTC on 15 July = 00:30 BST on 16 July - the UK day is the 16th.
            response.getClaim().setClaimIssuedDate(LocalDateTime.of(2026, 7, 15, 23, 30));

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getIssueDateSealed()).isEqualTo(LocalDate.of(2026, 7, 16));
        }

        @Test
        void usesPartyNameAndAddressWhenNotDisputed() {
            DefenceFormPayload payload = builder.build(response(LegislativeCountry.ENGLAND));
            assertThat(payload.getDefendantName()).isEqualTo("Bob Tenant");
            assertThat(payload.getDefendantAddress().getAddressLine1()).isEqualTo("42 Renters Way");
        }

        @Test
        void usesAssertedNameAndAddressWhenDisputed() {
            stubAssertions(
                jsonAssertion(PartyAttributeType.DEFENDANT_NAME, "{\"firstName\":\"Robert\",\"lastName\":\"Tennant\"}"),
                jsonAssertion(PartyAttributeType.CORRESPONDENCE_ADDRESS,
                    "{\"AddressLine1\":\"9 New Road\",\"PostTown\":\"Leeds\",\"PostCode\":\"LS1 1AA\"}"));

            DefenceFormPayload payload = builder.build(response(LegislativeCountry.ENGLAND));

            assertThat(payload.getDefendantName()).isEqualTo("Robert Tennant");
            assertThat(payload.getDefendantAddress().getAddressLine1()).isEqualTo("9 New Road");
            assertThat(payload.getDefendantAddress().getPostcode()).isEqualTo("LS1 1AA");
        }

        @Test
        void fallsBackToPropertyAddressWhenDefendantSameAsProperty() {
            // Defendant confirmed the claim-time address (no assertion) and was same-as-property,
            // so the party has no address of its own - the property address must fill the row.
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.getParty().setAddress(null);
            response.getPcsCase().setPropertyAddress(address("1 Second Avenue"));

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getDefendantAddress().getAddressLine1()).isEqualTo("1 Second Avenue");
        }

        @Test
        void neverRendersPersonsUnknownForRespondingDefendant() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.getParty().setFirstName(null);
            response.getParty().setLastName(null);
            response.getParty().setNameKnown(VerticalYesNo.NO);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getDefendantName()).doesNotContain("Persons unknown");
        }
    }

    @Nested
    class ResponseToClaim {

        @Test
        void hidesCorrectedTenancyTypeWhenConfirmed() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setTenancyTypeConfirmation(YesNoNotSure.YES);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getTenancyTypeConfirmation()).isEqualTo("Yes");
            assertThat(payload.isShowCorrectedTenancyType()).isFalse();
        }

        @Test
        void showsCorrectedTenancyTypeFromAssertionWhenDisputed() {
            stubAssertions(plainAssertion(PartyAttributeType.TENANCY_TYPE, "Assured shorthold"));
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setTenancyTypeConfirmation(YesNoNotSure.NO);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowCorrectedTenancyType()).isTrue();
            assertThat(payload.getCorrectedTenancyType()).isEqualTo("Assured shorthold");
        }

        @Test
        void showsCorrectedStartDateFromIsoAssertion() {
            stubAssertions(plainAssertion(PartyAttributeType.TENANCY_START_DATE, "2022-03-14"));
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setTenancyStartDateConfirmation(YesNoNotSure.NO);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowCorrectedStartDate()).isTrue();
            assertThat(payload.getCorrectedStartDate()).isEqualTo("14 March 2022");
            assertThat(payload.isShowStartDateConfirmation()).isTrue();
            assertThat(payload.isShowDefendantProvidedStartDate()).isFalse();
        }

        @Test
        void confirmedStartDate_showsConfirmationOnly() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setTenancyStartDateConfirmation(YesNoNotSure.YES);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowStartDateConfirmation()).isTrue();
            assertThat(payload.getTenancyStartDateConfirmation()).isEqualTo("Yes");
            assertThat(payload.isShowCorrectedStartDate()).isFalse();
            assertThat(payload.isShowDefendantProvidedStartDate()).isFalse();
        }

        @Test
        void claimantGaveNoStartDate_showsDefendantProvidedDateHidesConfirmation() {
            // Unknown branch: claimant left the start date blank, defendant supplied one (stored as
            // an assertion) without a confirmation. Show the provided date, hide the blank confirmation.
            stubAssertions(plainAssertion(PartyAttributeType.TENANCY_START_DATE, "2020-02-01"));
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setTenancyStartDateConfirmation(null);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowStartDateConfirmation()).isFalse();
            assertThat(payload.isShowCorrectedStartDate()).isFalse();
            assertThat(payload.isShowDefendantProvidedStartDate()).isTrue();
            assertThat(payload.getDefendantProvidedStartDate()).isEqualTo("1 February 2020");
        }

        @Test
        void writtenTermsShownForWalesOnly() {
            assertThat(builder.build(response(LegislativeCountry.WALES)).isShowWrittenTerms()).isTrue();
            assertThat(builder.build(response(LegislativeCountry.ENGLAND)).isShowWrittenTerms()).isFalse();
        }

        @Test
        void landlordRegisteredAndLicensedShownForWalesOnly() {
            DefendantResponseEntity wales = response(LegislativeCountry.WALES);
            wales.setLandlordRegistered(YesNoNotSure.YES);
            wales.setLandlordLicensed(YesNoNotSure.NO);

            DefenceFormPayload payload = builder.build(wales);

            assertThat(payload.isShowLandlordRegistered()).isTrue();
            assertThat(payload.getLandlordRegistered()).isEqualTo("Yes");
            assertThat(payload.isShowLandlordLicensed()).isTrue();
            assertThat(payload.getLandlordLicensed()).isEqualTo("No");

            DefenceFormPayload england = builder.build(response(LegislativeCountry.ENGLAND));
            assertThat(england.isShowLandlordRegistered()).isFalse();
            assertThat(england.isShowLandlordLicensed()).isFalse();
        }

        @Test
        void mapsNoticeAnswerAndDate() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setPossessionNoticeReceived(YesNoNotSure.YES);
            response.setNoticeReceivedDate(LocalDate.of(2025, 2, 1));

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getPossessionNoticeReceived()).isEqualTo("Yes");
            assertThat(payload.isShowNoticeReceivedDate()).isTrue();
            assertThat(payload.getNoticeReceivedDate()).isEqualTo("1 February 2025");
        }

        @Test
        void hidesNoticeDateWhenNoticeNotReceived() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setPossessionNoticeReceived(YesNoNotSure.NOT_SURE);
            response.setNoticeReceivedDate(LocalDate.of(2025, 2, 1));

            DefenceFormPayload payload = builder.build(response);

            // A stray date must not render when the defendant didn't say "Yes" to receiving a notice.
            assertThat(payload.isShowNoticeReceivedDate()).isFalse();
        }

        @Test
        void mapsAdditionalInformation() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setOtherConsiderations(VerticalYesNo.YES);
            response.setOtherConsiderationsDetails("Outstanding repairs");

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getOtherConsiderations()).isEqualTo("Yes");
            assertThat(payload.isShowAdditionalInfoDetails()).isTrue();
            assertThat(payload.getOtherConsiderationsDetails()).isEqualTo("Outstanding repairs");
        }

        @Test
        void hidesAdditionalInformationDetailsWhenNotRaised() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setOtherConsiderations(VerticalYesNo.NO);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getOtherConsiderations()).isEqualTo("No");
            assertThat(payload.isShowAdditionalInfoDetails()).isFalse();
        }

        @Test
        void rentArrearsAmountQuestionAndAdmittedAmountGatedByGroundAndAssertion() {
            stubAssertions(plainAssertion(PartyAttributeType.RENT_ARREARS_AMOUNT, "1200.50"));
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.getClaim().setClaimGrounds(Set.of(rentArrearsGround()));
            response.setRentArrearsAmountConfirmation(YesNoNotSure.NO);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowRentArrearsAmountQuestion()).isTrue();
            assertThat(payload.isShowAdmittedArrearsAmount()).isTrue();
            assertThat(payload.getAdmittedArrearsAmount()).isEqualTo("£1,200.50");
            // Only rent-arrears grounds -> "dispute other parts" hidden.
            assertThat(payload.isShowDisputeOtherParts()).isFalse();
        }

        @Test
        void disputeOtherPartsShownWhenNotAllGroundsAreRentArrears() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.getClaim().setClaimGrounds(Set.of(rentArrearsGround(), nonRentArrearsGround()));
            response.setDisputeClaim(VerticalYesNo.YES);
            response.setDisputeClaimDetails("Disputed");

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowDisputeOtherParts()).isTrue();
            assertThat(payload.isShowDisputeClaimDetails()).isTrue();
            assertThat(payload.getDisputeClaimDetails()).isEqualTo("Disputed");
        }
    }

    @Nested
    class Payments {

        @Test
        void paymentsSectionHiddenWithoutRentArrearsGround() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.getClaim().setClaimGrounds(Set.of(nonRentArrearsGround()));

            assertThat(builder.build(response).isShowPaymentsSection()).isFalse();
        }

        @Test
        void mapsPaymentAgreementFieldsWhenRentArrears() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.getClaim().setClaimGrounds(Set.of(rentArrearsGround()));
            response.setPaymentAgreement(PaymentAgreementEntity.builder()
                .anyPaymentsMade(VerticalYesNo.YES)
                .paymentDetails("Paid £200")
                .repaymentPlanAgreed(YesNoNotSure.NO)
                .repayArrearsInstalments(VerticalYesNo.YES)
                .additionalRentContribution(new BigDecimal("50.00"))
                .additionalContributionFrequency("every2Weeks")
                .build());

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowPaymentsSection()).isTrue();
            assertThat(payload.getAnyPaymentsMade()).isEqualTo("Yes");
            assertThat(payload.isShowPaymentDetails()).isTrue();
            assertThat(payload.getPaymentDetails()).isEqualTo("Paid £200");
            assertThat(payload.isShowOfferInstalments()).isTrue();
            assertThat(payload.isShowInstalmentAmount()).isTrue();
            assertThat(payload.getInstalmentAmount()).isEqualTo("£50.00");
            assertThat(payload.getInstalmentFrequency()).isEqualTo("Every 2 weeks");
        }
    }

    @Nested
    class HouseholdIncomeAndExpenses {

        @Test
        void mapsIncomeRowsExpensesAndMoneyFromElsewhere() {
            RegularIncomeEntity income = RegularIncomeEntity.builder()
                .otherIncomeDetails("Cash gifts")
                .build();
            income.addItem(RegularIncomeItemEntity.builder()
                .incomeType(IncomeType.INCOME_FROM_JOBS).amount(new BigDecimal("1500.00"))
                .frequency(RecurrenceFrequency.MONTHLY).build());
            income.addItem(RegularIncomeItemEntity.builder()
                .incomeType(IncomeType.MONEY_FROM_ELSEWHERE).build());

            HouseholdCircumstancesEntity household = HouseholdCircumstancesEntity.builder()
                .shareIncomeExpenseDetails(VerticalYesNo.YES)
                .universalCredit(VerticalYesNo.NO)
                .priorityDebts(VerticalYesNo.YES)
                .debtTotal(new BigDecimal("300.00"))
                .build();
            household.setRegularIncomeEntity(income);
            household.addRegularExpense(RegularExpenseEntity.builder()
                .expenseType(RegularExpenseType.OTHER).amount(new BigDecimal("25.00"))
                .expenseFrequency(RecurrenceFrequency.WEEKLY).build());

            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setHouseholdCircumstances(household);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowIncomeExpenseSection()).isTrue();
            assertThat(payload.getIncome()).hasSize(1);
            DefenceFormAmountRow incomeRow = payload.getIncome().getFirst();
            assertThat(incomeRow.getLabel()).isEqualTo("Income from all jobs you do");
            assertThat(incomeRow.getAmount()).isEqualTo("£1,500.00");
            assertThat(incomeRow.getFrequency()).isEqualTo("Monthly");

            assertThat(payload.isShowMoneyFromElsewhere()).isTrue();
            assertThat(payload.getMoneyFromElsewhereDetails()).isEqualTo("Cash gifts");

            assertThat(payload.isShowDebtDetails()).isTrue();
            assertThat(payload.getDebtTotal()).isEqualTo("£300.00");

            assertThat(payload.getExpenses()).hasSize(1);
            assertThat(payload.getExpenses().getFirst().getLabel()).isEqualTo("Other");
            assertThat(payload.getExpenses().getFirst().getAmount()).isEqualTo("£25.00");
        }

        @Test
        void expensesRenderInDesignOrderRegardlessOfInputOrder() {
            // Expenses persist in an unordered map; the form must list them in the design order.
            HouseholdCircumstancesEntity household = HouseholdCircumstancesEntity.builder()
                .shareIncomeExpenseDetails(VerticalYesNo.YES)
                .build();
            household.addRegularExpense(RegularExpenseEntity.builder()
                .expenseType(RegularExpenseType.CLOTHING).amount(new BigDecimal("10.00"))
                .expenseFrequency(RecurrenceFrequency.WEEKLY).build());
            household.addRegularExpense(RegularExpenseEntity.builder()
                .expenseType(RegularExpenseType.HOUSEHOLD_BILLS).amount(new BigDecimal("20.00"))
                .expenseFrequency(RecurrenceFrequency.MONTHLY).build());
            household.addRegularExpense(RegularExpenseEntity.builder()
                .expenseType(RegularExpenseType.OTHER).amount(new BigDecimal("5.00"))
                .expenseFrequency(RecurrenceFrequency.WEEKLY).build());
            household.addRegularExpense(RegularExpenseEntity.builder()
                .expenseType(RegularExpenseType.MOBILE_PHONE).amount(new BigDecimal("15.00"))
                .expenseFrequency(RecurrenceFrequency.MONTHLY).build());

            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setHouseholdCircumstances(household);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getExpenses()).extracting(DefenceFormAmountRow::getLabel)
                .containsExactly(
                    "Household bills (for example, council tax, gas, electricity, water, internet)",
                    "Mobile phone",
                    "Clothing",
                    "Other");
        }

        @Test
        void showsHouseholdDetailOnlyWhenAnswerIsYes() {
            HouseholdCircumstancesEntity household = HouseholdCircumstancesEntity.builder()
                .dependantChildren(VerticalYesNo.YES)
                .dependantChildrenDetails("Two children, aged 5 and 8")
                .otherTenants(VerticalYesNo.NO)
                .otherTenantsDetails(null)
                .exceptionalHardship(VerticalYesNo.YES)
                .exceptionalHardshipDetails("Sole carer for a disabled parent")
                .build();
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setHouseholdCircumstances(household);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowDependantChildrenDetails()).isTrue();
            assertThat(payload.getDependantChildrenDetails()).isEqualTo("Two children, aged 5 and 8");
            assertThat(payload.isShowExceptionalHardshipDetails()).isTrue();
            assertThat(payload.getExceptionalHardshipDetails()).isEqualTo("Sole carer for a disabled parent");
            // answered "No" -> no detail row at all
            assertThat(payload.isShowOtherTenantsDetails()).isFalse();
        }

        @Test
        void nullHouseholdLeavesSectionHidden() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setHouseholdCircumstances(null);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.isShowIncomeExpenseSection()).isFalse();
            assertThat(payload.getIncome()).isNull();
        }
    }

    @Nested
    class SubmittedDate {

        @Test
        void mapsSubmittedDateFromPersistedSubmissionTimestamp() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            // Stored as UTC; in BST (UTC+1) 23:30 the night before is the next UK calendar day.
            response.setResponseSubmittedDate(LocalDateTime.of(2026, 7, 15, 23, 30));

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getSubmittedOn()).isEqualTo(LocalDate.of(2026, 7, 16));
        }

        @Test
        void nullSubmissionTimestampLeavesSubmittedDateUnset() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setResponseSubmittedDate(null);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getSubmittedOn()).isNull();
        }
    }

    @Nested
    class StatementOfTruth {

        @Test
        void mapsFullName() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setStatementOfTruth(StatementOfTruthEntity.builder()
                .fullName("Bob Tenant")
                .completedDate(LocalDateTime.of(2026, 3, 1, 10, 30))
                .build());

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getSotFullName()).isEqualTo("Bob Tenant");
        }

        @Test
        void nullStatementOfTruthLeavesNameUnset() {
            DefendantResponseEntity response = response(LegislativeCountry.ENGLAND);
            response.setStatementOfTruth(null);

            DefenceFormPayload payload = builder.build(response);

            assertThat(payload.getSotFullName()).isNull();
        }
    }

    // ----- fixtures -----

    private void stubAssertions(PartyAttributeAssertationEntity... assertions) {
        lenient().when(assertionRepository.findByPartyIdAndAssertedByAndStatus(
                DEFENDANT_ID,
                PartyAttributeAssertedBy.DEFENDANT,
                PartyAttributeAssertionStatus.SUBMITTED))
            .thenReturn(List.of(assertions));
    }

    private static PartyAttributeAssertationEntity plainAssertion(PartyAttributeType type, String value) {
        return PartyAttributeAssertationEntity.builder().attributesName(type).assertedValue(value).build();
    }

    private static PartyAttributeAssertationEntity jsonAssertion(PartyAttributeType type, String json) {
        return PartyAttributeAssertationEntity.builder().attributesName(type).assertedValue(json).build();
    }

    private static ClaimGroundEntity rentArrearsGround() {
        return ClaimGroundEntity.builder().code("RENT_ARREARS").isRentArrears(true).build();
    }

    private static ClaimGroundEntity nonRentArrearsGround() {
        return ClaimGroundEntity.builder().code("ASB").isRentArrears(false).build();
    }

    private static DefendantResponseEntity response(LegislativeCountry country) {
        PartyEntity claimant = party("Alice", "Owner", address("1 High St"));
        PartyEntity defendant = party("Bob", "Tenant", address("42 Renters Way"));
        defendant.setId(DEFENDANT_ID);

        PcsCaseEntity pcsCase = PcsCaseEntity.builder()
            .caseReference(1234567812345678L)
            .legislativeCountry(country)
            .build();

        ClaimEntity claim = ClaimEntity.builder()
            .claimIssuedDate(LocalDateTime.of(2026, 1, 5, 9, 0))
            .claimParties(new ArrayList<>())
            .claimGrounds(new HashSet<>())
            .build();
        claim.getClaimParties().add(claimParty(claim, claimant, PartyRole.CLAIMANT, 1));
        claim.getClaimParties().add(claimParty(claim, defendant, PartyRole.DEFENDANT, 1));

        return DefendantResponseEntity.builder()
            .id(UUID.randomUUID())
            .claim(claim)
            .pcsCase(pcsCase)
            .party(defendant)
            .build();
    }

    private static ClaimPartyEntity claimParty(ClaimEntity claim, PartyEntity party, PartyRole role, int rank) {
        return ClaimPartyEntity.builder().claim(claim).party(party).role(role).rank(rank).build();
    }

    private static PartyEntity party(String firstName, String lastName, AddressEntity address) {
        return PartyEntity.builder()
            .id(UUID.randomUUID())
            .firstName(firstName)
            .lastName(lastName)
            .nameKnown(VerticalYesNo.YES)
            .address(address)
            .build();
    }

    private static AddressEntity address(String line1) {
        return AddressEntity.builder().addressLine1(line1).postTown("London").postcode("AB1 2CD").build();
    }
}
