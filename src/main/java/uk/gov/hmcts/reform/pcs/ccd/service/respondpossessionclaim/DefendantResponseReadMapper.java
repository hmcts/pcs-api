package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.IncomeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.CounterClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.IncomeExpenseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PaymentAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.CounterClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularExpenseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeItemEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import static uk.gov.hmcts.reform.pcs.ccd.util.YesOrNoConverter.toYesOrNo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class DefendantResponseReadMapper {

    private final AddressMapper addressMapper;

    public DefendantResponseReadMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public PossessionClaimResponse toPossessionClaimResponse(
        DefendantResponseEntity entity,
        List<PartyAttributeAssertationEntity> assertions
    ) {
        PartyEntity party = entity.getParty();
        PcsCaseEntity pcsCase = entity.getPcsCase();

        return PossessionClaimResponse.builder()
            .defendantContactDetails(toDefendantContactDetails(party, pcsCase))
            .defendantResponses(toDefendantResponses(entity, party, assertions))
            .currentDefendantPartyId(party.getId() != null ? party.getId().toString() : null)
            .claimIssuedDate(toClaimIssuedDate(entity.getClaim()))
            .build();
    }

    private static LocalDate toClaimIssuedDate(ClaimEntity claim) {
        return Optional.ofNullable(claim)
            .map(ClaimEntity::getClaimIssuedDate)
            .map(LocalDateTime::toLocalDate)
            .orElse(null);
    }

    private DefendantContactDetails toDefendantContactDetails(PartyEntity party, PcsCaseEntity pcsCase) {
        return DefendantContactDetails.builder()
            .party(toParty(party, pcsCase))
            .build();
    }

    private Party toParty(PartyEntity party, PcsCaseEntity pcsCase) {
        return Party.builder()
            .firstName(party.getFirstName())
            .lastName(party.getLastName())
            .emailAddress(party.getEmailAddress())
            .phoneNumber(party.getPhoneNumber())
            .phoneNumberProvided(party.getPhoneNumberProvided())
            .dateOfBirth(party.getDateOfBirth())
            .address(resolveAddress(party, pcsCase))
            .build();
    }

    private AddressUK resolveAddress(PartyEntity party, PcsCaseEntity pcsCase) {
        if (party.getAddressSameAsProperty() == VerticalYesNo.YES && pcsCase != null) {
            return Optional.ofNullable(pcsCase.getPropertyAddress())
                .map(addressMapper::toAddressUK)
                .orElse(AddressUK.builder().build());
        }
        return Optional.ofNullable(party.getAddress())
            .map(addressMapper::toAddressUK)
            .orElse(AddressUK.builder().build());
    }

    private DefendantResponses toDefendantResponses(
        DefendantResponseEntity entity,
        PartyEntity party,
        List<PartyAttributeAssertationEntity> assertions
    ) {
        DefendantResponses.DefendantResponsesBuilder builder = DefendantResponses.builder()
            .tenancyTypeConfirmation(entity.getTenancyTypeConfirmation())
            .tenancyStartDateConfirmation(entity.getTenancyStartDateConfirmation())
            .rentArrearsAmountConfirmation(entity.getRentArrearsAmountConfirmation())
            .landlordRegistered(entity.getLandlordRegistered())
            .landlordLicensed(entity.getLandlordLicensed())
            .writtenTerms(entity.getWrittenTerms())
            .disputeClaim(entity.getDisputeClaim())
            .disputeClaimDetails(entity.getDisputeClaimDetails())
            .makeCounterClaim(entity.getMakeCounterClaim())
            .otherConsiderations(entity.getOtherConsiderations())
            .otherConsiderationsDetails(entity.getOtherConsiderationsDetails())
            .languageUsed(entity.getLanguageUsed())
            .freeLegalAdvice(entity.getFreeLegalAdvice())
            .defendantNameConfirmation(entity.getDefendantNameConfirmation())
            .correspondenceAddressConfirmation(entity.getCorrespondenceAddressConfirmation())
            .statementOfTruthCompletedBy(mapStatementOfTruthCompletedBy(entity))
            .paymentAgreement(toPaymentAgreement(entity.getPaymentAgreement()))
            .householdCircumstances(toHouseholdCircumstances(entity.getHouseholdCircumstances()))
            .counterClaim(findCounterClaim(entity, party.getId()))
            .dateOfBirth(party.getDateOfBirth());

        applyContactPreferences(builder, party.getContactPreferences());
        applyPartyAssertions(builder, toAssertionValues(assertions));
        return builder.build();
    }

    private Map<PartyAttributeType, String> toAssertionValues(List<PartyAttributeAssertationEntity> assertions) {
        Map<PartyAttributeType, String> values = new EnumMap<>(PartyAttributeType.class);
        if (assertions == null) {
            return values;
        }

        assertions.forEach(assertion -> {
            if (assertion.getAttributesName() != null) {
                values.put(assertion.getAttributesName(), assertion.getAssertedValue());
            }
        });
        return values;
    }

    private void applyPartyAssertions(
        DefendantResponses.DefendantResponsesBuilder builder,
        Map<PartyAttributeType, String> values
    ) {
        String tenancyType = values.get(PartyAttributeType.TENANCY_TYPE);
        if (StringUtils.isNotBlank(tenancyType)) {
            builder.tenancyType(tenancyType);
        }

        String tenancyStartDate = values.get(PartyAttributeType.TENANCY_START_DATE);
        if (StringUtils.isNotBlank(tenancyStartDate)) {
            builder.tenancyStartDate(LocalDate.parse(tenancyStartDate));
        }

        String rentArrearsAmount = values.get(PartyAttributeType.RENT_ARREARS_AMOUNT);
        if (StringUtils.isNotBlank(rentArrearsAmount)) {
            builder.rentArrearsAmount(new BigDecimal(rentArrearsAmount));
        }
    }

    private static String mapStatementOfTruthCompletedBy(DefendantResponseEntity entity) {
        return Optional.ofNullable(entity.getStatementOfTruth())
            .map(StatementOfTruthEntity::getFullName)
            .filter(StringUtils::isNotBlank)
            .orElse(null);
    }

    private void applyContactPreferences(
        DefendantResponses.DefendantResponsesBuilder builder,
        ContactPreferencesEntity prefs
    ) {
        if (prefs == null) {
            return;
        }
        builder
            .contactByEmail(prefs.getContactByEmail())
            .contactByPost(prefs.getContactByPost())
            .contactByPhone(prefs.getContactByPhone())
            .contactByText(prefs.getContactByText());
    }

    private PaymentAgreement toPaymentAgreement(PaymentAgreementEntity entity) {
        if (entity == null) {
            return null;
        }
        return PaymentAgreement.builder()
            .anyPaymentsMade(entity.getAnyPaymentsMade())
            .paymentDetails(entity.getPaymentDetails())
            .repaymentPlanAgreed(entity.getRepaymentPlanAgreed())
            .repaymentAgreedDetails(entity.getRepaymentAgreedDetails())
            .repayArrearsInstalments(entity.getRepayArrearsInstalments())
            .additionalRentContribution(entity.getAdditionalRentContribution())
            .additionalContributionFrequency(entity.getAdditionalContributionFrequency())
            .build();
    }

    private HouseholdCircumstances toHouseholdCircumstances(HouseholdCircumstancesEntity entity) {
        if (entity == null) {
            return null;
        }

        HouseholdCircumstances.HouseholdCircumstancesBuilder builder = HouseholdCircumstances.builder()
            .dependantChildren(toYesOrNo(entity.getDependantChildren()))
            .dependantChildrenDetails(entity.getDependantChildrenDetails())
            .otherDependants(toYesOrNo(entity.getOtherDependants()))
            .otherDependantDetails(entity.getOtherDependantDetails())
            .otherTenants(toYesOrNo(entity.getOtherTenants()))
            .otherTenantsDetails(entity.getOtherTenantsDetails())
            .alternativeAccommodation(entity.getAlternativeAccommodation())
            .alternativeAccommodationTransferDate(entity.getAlternativeAccommodationTransferDate())
            .shareAdditionalCircumstances(toYesOrNo(entity.getShareAdditionalCircumstances()))
            .additionalCircumstancesDetails(entity.getAdditionalCircumstancesDetails())
            .exceptionalHardship(toYesOrNo(entity.getExceptionalHardship()))
            .exceptionalHardshipDetails(entity.getExceptionalHardshipDetails())
            .shareIncomeExpenseDetails(toYesOrNo(entity.getShareIncomeExpenseDetails()))
            .universalCredit(toYesOrNo(entity.getUniversalCredit()))
            .ucApplicationDate(entity.getUcApplicationDate())
            .priorityDebts(toYesOrNo(entity.getPriorityDebts()))
            .debtTotal(entity.getDebtTotal())
            .debtContribution(entity.getDebtContribution())
            .debtContributionFrequency(entity.getDebtContributionFrequency());

        applyRegularExpenses(builder, entity.getRegularExpenses());
        applyRegularIncome(builder, entity.getRegularIncomeEntity(), entity);

        return builder.build();
    }

    private void applyRegularExpenses(
        HouseholdCircumstances.HouseholdCircumstancesBuilder builder,
        List<RegularExpenseEntity> expenses
    ) {
        if (expenses == null) {
            return;
        }
        for (RegularExpenseEntity expense : expenses) {
            IncomeExpenseDetails details = IncomeExpenseDetails.builder()
                .applies(YesOrNo.YES)
                .amount(expense.getAmount())
                .frequency(expense.getExpenseFrequency())
                .build();
            switch (expense.getExpenseType()) {
                case HOUSEHOLD_BILLS -> builder.householdBills(details);
                case LOAN_PAYMENTS -> builder.loanPayments(details);
                case CHILD_SPOUSAL_MAINTENANCE -> builder.childSpousalMaintenance(details);
                case MOBILE_PHONE -> builder.mobilePhone(details);
                case GROCERY_SHOPPING -> builder.groceryShopping(details);
                case FUEL_PARKING_TRANSPORT -> builder.fuelParkingTransport(details);
                case SCHOOL_COSTS -> builder.schoolCosts(details);
                case CLOTHING -> builder.clothing(details);
                case OTHER -> builder.otherExpenses(details);
                default -> { /* no-op */ }
            }
        }
    }

    private void applyRegularIncome(
        HouseholdCircumstances.HouseholdCircumstancesBuilder builder,
        RegularIncomeEntity regularIncome,
        HouseholdCircumstancesEntity hcEntity
    ) {
        boolean hasUniversalCreditItem = false;

        if (regularIncome != null && regularIncome.getItems() != null) {
            for (RegularIncomeItemEntity item : regularIncome.getItems()) {
                if (item.getIncomeType() == IncomeType.UNIVERSAL_CREDIT) {
                    hasUniversalCreditItem = true;
                }
                applyIncomeItem(builder, item);
            }
            if (regularIncome.getOtherIncomeDetails() != null) {
                builder.moneyFromElsewhere(YesOrNo.YES);
                builder.moneyFromElsewhereDetails(regularIncome.getOtherIncomeDetails());
            }
        }

        if (hcEntity.getUcApplicationDate() != null) {
            builder.hasAppliedForUniversalCredit(YesOrNo.YES);
            builder.ucApplicationDate(hcEntity.getUcApplicationDate());
        } else if (hcEntity.getUniversalCredit() == VerticalYesNo.YES && !hasUniversalCreditItem) {
            builder.universalCredit(YesOrNo.YES);
        }
    }

    private void applyIncomeItem(
        HouseholdCircumstances.HouseholdCircumstancesBuilder builder,
        RegularIncomeItemEntity item
    ) {
        if (item.getIncomeType() == null) {
            return;
        }
        switch (item.getIncomeType()) {
            case INCOME_FROM_JOBS -> builder
                .incomeFromJobs(YesOrNo.YES)
                .incomeFromJobsAmount(item.getAmount())
                .incomeFromJobsFrequency(item.getFrequency());
            case PENSION -> builder
                .pension(YesOrNo.YES)
                .pensionAmount(item.getAmount())
                .pensionFrequency(item.getFrequency());
            case UNIVERSAL_CREDIT -> builder
                .universalCredit(YesOrNo.YES)
                .universalCreditAmount(item.getAmount())
                .universalCreditFrequency(item.getFrequency());
            case OTHER_BENEFITS -> builder
                .otherBenefits(YesOrNo.YES)
                .otherBenefitsAmount(item.getAmount())
                .otherBenefitsFrequency(item.getFrequency());
            case MONEY_FROM_ELSEWHERE -> builder.moneyFromElsewhere(YesOrNo.YES);
            default -> { /* no-op */ }
        }
    }

    private CounterClaim findCounterClaim(DefendantResponseEntity entity, UUID partyId) {
        PcsCaseEntity pcsCase = entity.getPcsCase();
        if (pcsCase == null || pcsCase.getCounterClaims() == null || partyId == null) {
            return null;
        }
        return pcsCase.getCounterClaims().stream()
            .filter(cc -> cc.getParty() != null && partyId.equals(cc.getParty().getId()))
            .findFirst()
            .map(this::toCounterClaim)
            .orElse(null);
    }

    private CounterClaim toCounterClaim(CounterClaimEntity entity) {
        return CounterClaim.builder()
            .claimType(entity.getClaimType())
            .isClaimAmountKnown(entity.getIsClaimAmountKnown())
            .claimAmount(entity.getClaimAmount())
            .estimatedMaxClaimAmount(entity.getEstimatedMaxClaimAmount())
            .needHelpWithFees(entity.getNeedHelpWithFees())
            .counterClaimFor(entity.getCounterClaimFor())
            .counterClaimReasons(entity.getCounterClaimReasons())
            .build();
    }
}