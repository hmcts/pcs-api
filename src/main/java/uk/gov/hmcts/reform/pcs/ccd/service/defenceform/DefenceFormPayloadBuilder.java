package uk.gov.hmcts.reform.pcs.ccd.service.defenceform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.IncomeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertionStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.RegularIncomeItemEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAttributeAssertionRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.PartyDisplayMapper;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormAddress;
import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormAmountRow;
import uk.gov.hmcts.reform.pcs.document.model.defenceform.DefenceFormPayload;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatAdditionalContributionFrequency;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatFrequency;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatGbp;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatIsoDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.formatLongDate;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.isNo;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.isPopulated;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.isYes;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.toFormAddress;
import static uk.gov.hmcts.reform.pcs.ccd.service.defenceform.DefenceFormFormatter.toLabel;

/**
 * Builds {@link DefenceFormPayload} from a {@link DefendantResponseEntity} and its claim baseline.
 *
 * <p>One {@code mapXxx} method per source. Where the defendant disputed a value, the form shows the
 * defendant's assertion ({@code party_attribute_assertion}); otherwise it falls back to the claim
 * baseline. Optional answers with no value leave their {@code show*} flag false so the template hides
 * the row.</p>
 */
@Service
@Slf4j
public class DefenceFormPayloadBuilder {

    private final CaseReferenceFormatter caseReferenceFormatter;
    private final CaseNameFormatter caseNameFormatter;
    private final PartyAttributeAssertionRepository assertionRepository;
    private final ObjectMapper objectMapper;

    private static final List<IncomeType> INCOME_ROW_ORDER = List.of(
        IncomeType.INCOME_FROM_JOBS,
        IncomeType.PENSION,
        IncomeType.UNIVERSAL_CREDIT,
        IncomeType.OTHER_BENEFITS
    );

    public DefenceFormPayloadBuilder(CaseReferenceFormatter caseReferenceFormatter,
                                     CaseNameFormatter caseNameFormatter,
                                     PartyAttributeAssertionRepository assertionRepository,
                                     ObjectMapper objectMapper) {
        this.caseReferenceFormatter = caseReferenceFormatter;
        this.caseNameFormatter = caseNameFormatter;
        this.assertionRepository = assertionRepository;
        this.objectMapper = objectMapper;
    }

    public DefenceFormPayload build(DefendantResponseEntity response) {
        ClaimEntity claim = response.getClaim();
        PcsCaseEntity pcsCase = response.getPcsCase();
        PartyEntity defendant = response.getParty();
        boolean isWales = pcsCase.getLegislativeCountry() == LegislativeCountry.WALES;

        List<ClaimGroundEntity> grounds = grounds(claim);
        boolean hasRentArrearsGround = grounds.stream().anyMatch(DefenceFormPayloadBuilder::isRentArrears);
        boolean hasOnlyRentArrearsGrounds =
            !grounds.isEmpty() && grounds.stream().allMatch(DefenceFormPayloadBuilder::isRentArrears);

        Map<PartyAttributeType, PartyAttributeAssertationEntity> assertions = loadAssertions(defendant);

        DefenceFormPayload.DefenceFormPayloadBuilder payload = DefenceFormPayload.builder();

        mapCaseAndParties(claim, pcsCase, defendant, isWales, assertions, payload);
        mapResponse(response, isWales, hasRentArrearsGround, hasOnlyRentArrearsGrounds, assertions, payload);
        mapPaymentAgreement(response.getPaymentAgreement(), hasRentArrearsGround, payload);
        mapHouseholdCircumstances(response.getHouseholdCircumstances(), payload);
        mapStatementOfTruth(response.getStatementOfTruth(), payload);

        return payload.build();
    }

    private void mapCaseAndParties(ClaimEntity claim, PcsCaseEntity pcsCase, PartyEntity defendant,
                                   boolean isWales,
                                   Map<PartyAttributeType, PartyAttributeAssertationEntity> assertions,
                                   DefenceFormPayload.DefenceFormPayloadBuilder payload) {
        payload.referenceNumber(caseReferenceFormatter.formatCaseReferenceWithDashes(pcsCase.getCaseReference()));
        payload.isWales(isWales);
        payload.isEngland(!isWales);

        List<PartyEntity> claimants = partiesByRole(claim, PartyRole.CLAIMANT);
        List<PartyEntity> defendants = partiesByRole(claim, PartyRole.DEFENDANT);
        payload.caseName(caseNameFormatter.formatCaseName(toDomainParties(claimants), toDomainParties(defendants)));

        if (claim.getClaimIssuedDate() != null) {
            payload.issueDateSealed(claim.getClaimIssuedDate().toLocalDate());
        }

        if (!claimants.isEmpty()) {
            PartyEntity claimant = claimants.getFirst();
            payload.claimantName(displayName(claimant));
            ClaimFormAddress claimantAddress = toFormAddress(claimant.getAddress());
            payload.claimantAddress(claimantAddress);
            applyAddressFlags(claimantAddress,
                payload::hasClaimantAddressLine2, payload::hasClaimantAddressLine3, payload::hasClaimantCounty);
        }

        payload.defendantName(resolveDefendantName(defendant, assertions));
        ClaimFormAddress defendantAddress = resolveDefendantAddress(defendant, assertions);
        payload.defendantAddress(defendantAddress);
        applyAddressFlags(defendantAddress,
            payload::hasDefendantAddressLine2, payload::hasDefendantAddressLine3, payload::hasDefendantCounty);
    }

    private void mapResponse(DefendantResponseEntity response, boolean isWales,
                             boolean hasRentArrearsGround, boolean hasOnlyRentArrearsGrounds,
                             Map<PartyAttributeType, PartyAttributeAssertationEntity> assertions,
                             DefenceFormPayload.DefenceFormPayloadBuilder payload) {
        payload.tenancyTypeConfirmation(toLabel(response.getTenancyTypeConfirmation()));
        String correctedType = assertedValue(assertions, PartyAttributeType.TENANCY_TYPE);
        payload.showCorrectedTenancyType(isNo(response.getTenancyTypeConfirmation()) && isPopulated(correctedType));
        payload.correctedTenancyType(correctedType);

        payload.tenancyStartDateConfirmation(toLabel(response.getTenancyStartDateConfirmation()));
        String correctedStartDate = assertedValue(assertions, PartyAttributeType.TENANCY_START_DATE);
        payload.showCorrectedStartDate(
            isNo(response.getTenancyStartDateConfirmation()) && isPopulated(correctedStartDate));
        payload.correctedStartDate(formatIsoDate(correctedStartDate));

        payload.showLandlordRegistered(isWales);
        payload.landlordRegistered(toLabel(response.getLandlordRegistered()));
        payload.showLandlordLicensed(isWales);
        payload.landlordLicensed(toLabel(response.getLandlordLicensed()));
        payload.showWrittenTerms(isWales);
        payload.writtenTerms(toLabel(response.getWrittenTerms()));

        payload.possessionNoticeReceived(toLabel(response.getPossessionNoticeReceived()));
        payload.showNoticeReceivedDate(
            isYes(response.getPossessionNoticeReceived()) && response.getNoticeReceivedDate() != null);
        payload.noticeReceivedDate(formatLongDate(response.getNoticeReceivedDate()));

        payload.showRentArrearsAmountQuestion(hasRentArrearsGround);
        payload.rentArrearsAmountConfirmation(toLabel(response.getRentArrearsAmountConfirmation()));
        String admittedArrears = assertedValue(assertions, PartyAttributeType.RENT_ARREARS_AMOUNT);
        payload.showAdmittedArrearsAmount(
            isNo(response.getRentArrearsAmountConfirmation()) && isPopulated(admittedArrears));
        payload.admittedArrearsAmount(isPopulated(admittedArrears) ? formatGbp(new BigDecimal(admittedArrears)) : null);

        payload.showDisputeOtherParts(!hasOnlyRentArrearsGrounds);
        payload.disputeClaim(toLabel(response.getDisputeClaim()));
        payload.showDisputeClaimDetails(isYes(response.getDisputeClaim()));
        payload.disputeClaimDetails(response.getDisputeClaimDetails());

        payload.otherConsiderations(toLabel(response.getOtherConsiderations()));
        payload.showAdditionalInfoDetails(isYes(response.getOtherConsiderations()));
        payload.otherConsiderationsDetails(response.getOtherConsiderationsDetails());
    }

    private void mapPaymentAgreement(PaymentAgreementEntity payment, boolean hasRentArrearsGround,
                                     DefenceFormPayload.DefenceFormPayloadBuilder payload) {
        payload.showPaymentsSection(hasRentArrearsGround);
        if (payment == null) {
            return;
        }
        payload.anyPaymentsMade(toLabel(payment.getAnyPaymentsMade()));
        payload.showPaymentDetails(isYes(payment.getAnyPaymentsMade()));
        payload.paymentDetails(payment.getPaymentDetails());

        payload.repaymentPlanAgreed(toLabel(payment.getRepaymentPlanAgreed()));
        payload.showRepaymentAgreedDetails(isYes(payment.getRepaymentPlanAgreed()));
        payload.repaymentAgreedDetails(payment.getRepaymentAgreedDetails());
        payload.showOfferInstalments(isNo(payment.getRepaymentPlanAgreed()));

        payload.repayArrearsInstalments(toLabel(payment.getRepayArrearsInstalments()));
        payload.showInstalmentAmount(isYes(payment.getRepayArrearsInstalments()));
        payload.instalmentAmount(formatGbp(payment.getAdditionalRentContribution()));
        payload.instalmentFrequency(
            formatAdditionalContributionFrequency(payment.getAdditionalContributionFrequency()));
    }

    private void mapHouseholdCircumstances(HouseholdCircumstancesEntity household,
                                           DefenceFormPayload.DefenceFormPayloadBuilder payload) {
        if (household == null) {
            return;
        }
        payload.dependantChildren(toLabel(household.getDependantChildren()));
        payload.showDependantChildrenDetails(isYes(household.getDependantChildren()));
        payload.dependantChildrenDetails(household.getDependantChildrenDetails());
        payload.otherDependants(toLabel(household.getOtherDependants()));
        payload.showOtherDependantsDetails(isYes(household.getOtherDependants()));
        payload.otherDependantsDetails(household.getOtherDependantDetails());
        payload.otherTenants(toLabel(household.getOtherTenants()));
        payload.showOtherTenantsDetails(isYes(household.getOtherTenants()));
        payload.otherTenantsDetails(household.getOtherTenantsDetails());
        payload.alternativeAccommodation(toLabel(household.getAlternativeAccommodation()));
        payload.showTransferDate(household.getAlternativeAccommodationTransferDate() != null);
        payload.transferDate(formatLongDate(household.getAlternativeAccommodationTransferDate()));
        payload.shareAdditionalCircumstances(toLabel(household.getShareAdditionalCircumstances()));
        payload.showAdditionalCircumstancesDetails(isYes(household.getShareAdditionalCircumstances()));
        payload.additionalCircumstancesDetails(household.getAdditionalCircumstancesDetails());
        payload.exceptionalHardship(toLabel(household.getExceptionalHardship()));
        payload.showExceptionalHardshipDetails(isYes(household.getExceptionalHardship()));
        payload.exceptionalHardshipDetails(household.getExceptionalHardshipDetails());

        payload.showIncomeExpenseSection(isYes(household.getShareIncomeExpenseDetails()));
        mapRegularIncome(household.getRegularIncomeEntity(), payload);

        payload.showAppliedForUniversalCredit(isNo(household.getUniversalCredit()));
        payload.appliedForUniversalCredit(toLabel(household.getHasAppliedForUniversalCredit()));
        payload.showUcApplicationDate(household.getUcApplicationDate() != null);
        payload.ucApplicationDate(formatLongDate(household.getUcApplicationDate()));

        payload.priorityDebts(toLabel(household.getPriorityDebts()));
        payload.showDebtDetails(isYes(household.getPriorityDebts()));
        payload.debtTotal(formatGbp(household.getDebtTotal()));
        payload.debtContribution(formatGbp(household.getDebtContribution()));
        payload.debtContributionFrequency(formatFrequency(household.getDebtContributionFrequency()));

        payload.expenses(household.getRegularExpenses().stream()
            .map(expense -> amountRow(expense.getExpenseType().getLabel(),
                expense.getAmount(), formatFrequency(expense.getExpenseFrequency())))
            .toList());
    }

    private void mapRegularIncome(RegularIncomeEntity regularIncome,
                                  DefenceFormPayload.DefenceFormPayloadBuilder payload) {
        if (regularIncome == null) {
            payload.income(List.of());
            return;
        }
        Map<IncomeType, RegularIncomeItemEntity> byType = new EnumMap<>(IncomeType.class);
        regularIncome.getItems().forEach(item -> byType.put(item.getIncomeType(), item));

        payload.income(INCOME_ROW_ORDER.stream()
            .filter(byType::containsKey)
            .map(byType::get)
            .map(item -> amountRow(item.getIncomeType().getLabel(),
                item.getAmount(), formatFrequency(item.getFrequency())))
            .toList());

        payload.showMoneyFromElsewhere(byType.containsKey(IncomeType.MONEY_FROM_ELSEWHERE));
        payload.moneyFromElsewhereDetails(regularIncome.getOtherIncomeDetails());
    }

    private void mapStatementOfTruth(StatementOfTruthEntity statementOfTruth,
                                     DefenceFormPayload.DefenceFormPayloadBuilder payload) {
        if (statementOfTruth == null) {
            return;
        }
        if (statementOfTruth.getCompletedDate() != null) {
            payload.submittedOn(statementOfTruth.getCompletedDate().toLocalDate());
        }
        payload.sotFullName(statementOfTruth.getFullName());
    }

    private static DefenceFormAmountRow amountRow(String label, BigDecimal amount, String frequency) {
        return DefenceFormAmountRow.builder()
            .label(label)
            .showAmount(amount != null)
            .amount(formatGbp(amount))
            .frequency(frequency)
            .build();
    }

    private Map<PartyAttributeType, PartyAttributeAssertationEntity> loadAssertions(PartyEntity defendant) {
        Map<PartyAttributeType, PartyAttributeAssertationEntity> byType =
            new EnumMap<>(PartyAttributeType.class);
        assertionRepository.findByPartyIdAndAssertedByAndStatus(
                defendant.getId(),
                PartyAttributeAssertedBy.DEFENDANT,
                PartyAttributeAssertionStatus.SUBMITTED)
            .forEach(assertion -> byType.put(assertion.getAttributesName(), assertion));
        return byType;
    }

    private static String assertedValue(Map<PartyAttributeType, PartyAttributeAssertationEntity> assertions,
                                        PartyAttributeType type) {
        PartyAttributeAssertationEntity assertion = assertions.get(type);
        return assertion == null ? null : assertion.getAssertedValue();
    }

    // The responding defendant is always named, so this never falls back to "Persons unknown":
    // it uses the disputed name assertion when present, else the claim's party name.
    private String resolveDefendantName(PartyEntity defendant,
                                        Map<PartyAttributeType, PartyAttributeAssertationEntity> assertions) {
        String assertedName = assertedValue(assertions, PartyAttributeType.DEFENDANT_NAME);
        if (isPopulated(assertedName)) {
            try {
                JsonNode node = objectMapper.readTree(assertedName);
                String name = joinName(text(node, "firstName"), text(node, "lastName"));
                if (isPopulated(name)) {
                    return name;
                }
            } catch (Exception e) {
                log.error("Failed to parse defendant name assertion", e);
            }
        }
        return displayName(defendant);
    }

    private ClaimFormAddress resolveDefendantAddress(
        PartyEntity defendant, Map<PartyAttributeType, PartyAttributeAssertationEntity> assertions) {
        String assertedAddress = assertedValue(assertions, PartyAttributeType.CORRESPONDENCE_ADDRESS);
        if (isPopulated(assertedAddress)) {
            try {
                return toFormAddress(objectMapper.readValue(assertedAddress, AddressUK.class));
            } catch (Exception e) {
                log.error("Failed to parse defendant correspondence address assertion", e);
            }
        }
        return toFormAddress(defendant.getAddress());
    }

    private static void applyAddressFlags(ClaimFormAddress address,
                                          BooleanSetter line2, BooleanSetter line3, BooleanSetter county) {
        if (address == null) {
            return;
        }
        line2.set(isPopulated(address.getAddressLine2()));
        line3.set(isPopulated(address.getAddressLine3()));
        county.set(isPopulated(address.getCounty()));
    }

    private static String displayName(PartyEntity party) {
        if (isPopulated(party.getOrgName())) {
            return party.getOrgName();
        }
        return joinName(party.getFirstName(), party.getLastName());
    }

    private static String joinName(String firstName, String lastName) {
        return PartyDisplayMapper.joinName(firstName, lastName);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static boolean isRentArrears(ClaimGroundEntity ground) {
        return Boolean.TRUE.equals(ground.getIsRentArrears());
    }

    private static List<ClaimGroundEntity> grounds(ClaimEntity claim) {
        Collection<ClaimGroundEntity> grounds = claim.getClaimGrounds();
        return grounds == null ? List.of() : List.copyOf(grounds);
    }

    private static List<PartyEntity> partiesByRole(ClaimEntity claim, PartyRole role) {
        return PartyDisplayMapper.partiesByRole(claim, role);
    }

    private static List<Party> toDomainParties(List<PartyEntity> parties) {
        return parties.stream().map(PartyDisplayMapper::toDomainParty).toList();
    }

    @FunctionalInterface
    private interface BooleanSetter {
        void set(boolean value);
    }

}
