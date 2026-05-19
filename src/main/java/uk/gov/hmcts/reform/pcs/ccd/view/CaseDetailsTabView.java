package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyHousingAct;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ActionsTakenTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ApplicationsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ClaimTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ClaimantContactTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CostsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.DefendantCircumstanceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.DemotionOfTenancyTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.SuspensionOfRightToBuyTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.TenancyLicenceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.UnderlesseeOrMortgageInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ClaimantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.DefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.GroundsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ReasonsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.AdditionalDefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.DefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.GroundsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ReasonsForPossessionTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.RentArrearsTabDetailsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.ASSURED_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.DEMOTED_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.INTRODUCTORY_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.OTHER;

@AllArgsConstructor
@Component
public class CaseDetailsTabView {

    private static final String NO_ANSWER = " ";
    private static final DateTimeFormatter PATTERN = DateTimeFormatter.ofPattern("d MMMM yyyy");

    private final GroundsBuilder groundsBuilder;
    private final RentArrearsTabDetailsBuilder rentArrearsTabDetailsBuilder;
    private final ReasonsForPossessionTabDetailsBuilder reasonsForPossessionTabDetailsBuilder;
    private final ClaimantInformationTabDetailsBuilder claimantInformationTabDetailsBuilder;
    private final DefendantInformationTabDetailsBuilder defendantInformationTabDetailsBuilder;
    private final AdditionalDefendantInformationTabDetailsBuilder additionalDefendantInformationTabDetailsBuilder;

    public CaseDetailsTab buildCaseDetailsTab(PCSCase pcsCase) {
        ClaimTabDetails claimTabDetails = buildClaimTabDetails(pcsCase);
        GroundsForPossessionTabDetails groundsForPossessionTabDetails = buildGroundsForPossessionTabDetails(pcsCase);
        TenancyLicenceTabDetails tenancyLicenceTabDetails = buildTenancyLicenceTabDetails(pcsCase);
        NoticeTabDetails noticeTabDetails = buildNoticeTabDetails(pcsCase);
        ActionsTakenTabDetails actionsTakenTabDetails = buildActionsTakenTabDetails(pcsCase);
        RentArrearsTabDetails rentArrearsTabDetails = buildRentArrearsTabDetails(pcsCase);
        CostsTabDetails costsTabDetails = buildCostsTabDetails(pcsCase);
        ReasonsForPossessionTabDetails reasonsForPossessionTabDetails = buildReasonsForPossession(pcsCase);
        ApplicationsTabDetails applicationsTabDetails = buildApplicationsTabDetails(pcsCase);
        ClaimantInformationTabDetails claimantInformationTabDetails = buildClaimantInformationTabDetails(pcsCase);
        DefendantInformationTabDetails defendantInformationTabDetails =
            defendantInformationTabDetailsBuilder.createDetailedDefendantDetails(pcsCase);
        List<ListValue<UnderlesseeOrMortgageInformationTabDetails>> underlesseeMortgageTabDetailsList =
            buildUnderlesseeMortgageTabDetailsList(pcsCase);
        DemotionOfTenancyTabDetails demotionOfTenancyTabDetails = buildDemotionOfTenancyTabDetails(pcsCase);
        SuspensionOfRightToBuyTabDetails suspensionOfRightToBuyTabDetails =
            buildSuspensionOfRightToBuyTabDetails(pcsCase);

        CaseDetailsTab caseDetailsTab = CaseDetailsTab.builder()
            .claimDetails(claimTabDetails)
            .propertyAddress(pcsCase.getPropertyAddress())
            .groundsForPossessionDetails(groundsForPossessionTabDetails)
            .tenancyLicenceDetails(tenancyLicenceTabDetails)
            .noticeDetails(noticeTabDetails)
            .actionsTakenDetails(actionsTakenTabDetails)
            .rentArrearsDetails(rentArrearsTabDetails)
            .costsDetails(costsTabDetails)
            .reasonsForPossessionDetails(reasonsForPossessionTabDetails)
            .applicationsDetails(applicationsTabDetails)
            .claimantInformation(claimantInformationTabDetails)
            .defendantInformationDetails(defendantInformationTabDetails)
            .mortgageDetails(underlesseeMortgageTabDetailsList)
            .demotionOfTenancyDetails(demotionOfTenancyTabDetails)
            .suspensionOfRightToBuyDetails(suspensionOfRightToBuyTabDetails)
            .build();

        if (claimantInformationTabDetails != null) {
            caseDetailsTab.setClaimantAddress(getClaimantAddress(pcsCase));
            caseDetailsTab.setClaimantContactDetails(buildClaimantContactTabDetails(pcsCase));
        }

        if (defendantInformationTabDetails != null) {
            List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendantInformationTabDetails =
                additionalDefendantInformationTabDetailsBuilder.buildDetailedAdditionalDefendantsDetails(pcsCase);
            caseDetailsTab.setAdditionalDefendants(additionalDefendantInformationTabDetails);
            caseDetailsTab.setDefendantCircumstanceDetails(buildDefendantCircumstanceTabDetails(pcsCase));
        }
        return caseDetailsTab;
    }

    private ClaimTabDetails buildClaimTabDetails(PCSCase pcsCase) {
        String claimantType = pcsCase.getClaimantType() != null
            ? ClaimantType.fromName(pcsCase.getClaimantType().getValueCode()).getLabel() : NO_ANSWER;
        VerticalYesNo trespassClaim = pcsCase.getClaimAgainstTrespassers();

        return ClaimTabDetails.builder()
            .claimantType(claimantType)
            .trespassClaim(trespassClaim != null ? trespassClaim.getLabel() : NO_ANSWER)
            .build();
    }

    private GroundsForPossessionTabDetails buildGroundsForPossessionTabDetails(PCSCase pcsCase) {
        List<ListValue<ClaimGroundSummary>> groundSummaries = pcsCase.getClaimGroundSummaries();
        if (CollectionUtils.isEmpty(groundSummaries)) {
            return GroundsForPossessionTabDetails.builder()
                .grounds(NO_ANSWER)
                .build();
        }

        TenancyLicenceDetails tenancyLicenceDetails = pcsCase.getTenancyLicenceDetails();
        TenancyLicenceType tenancyType = tenancyLicenceDetails != null ?
            tenancyLicenceDetails.getTypeOfTenancyLicence() : null;
        String otherGroundsDescription = "";

        if (tenancyType == INTRODUCTORY_TENANCY || tenancyType == DEMOTED_TENANCY || tenancyType == OTHER) {
            otherGroundsDescription = groundSummaries.stream().filter(
                claimGroundSummaryListValue -> {
                    ClaimGroundSummary claimGroundSummary = claimGroundSummaryListValue.getValue();
                    return claimGroundSummary.getCode().equals(IntroductoryDemotedOrOtherGrounds.OTHER.name());
                })
                .map(ListValue::getValue)
                .map(ClaimGroundSummary::getDescription)
                .findFirst()
                .orElse("");
        } else if (tenancyType == ASSURED_TENANCY) {
            otherGroundsDescription = groundSummaries.stream().filter(
                    claimGroundSummaryListValue -> {
                        ClaimGroundSummary claimGroundSummary = claimGroundSummaryListValue.getValue();
                        return claimGroundSummary.getCode().equals(AssuredAdditionalOtherGround.OTHER.name());
                    })
                .map(ListValue::getValue)
                .map(ClaimGroundSummary::getDescription)
                .findFirst()
                .orElse("");
        }

        return GroundsForPossessionTabDetails
            .builder()
            .grounds(groundsBuilder.getGrounds(pcsCase))
            .otherGroundsDescription(otherGroundsDescription)
            .build();
    }

    private TenancyLicenceTabDetails buildTenancyLicenceTabDetails(PCSCase pcsCase) {
        TenancyLicenceDetails tenancyLicenceDetails = pcsCase.getTenancyLicenceDetails();
        if (tenancyLicenceDetails == null) {
            return TenancyLicenceTabDetails.builder()
                .typeOfTenancyLicence(NO_ANSWER)
                .tenancyLicenceDate(NO_ANSWER)
                .hasCopyOfTenancyLicence(NO_ANSWER)
                .build();
        }

        TenancyLicenceType tenancyType = tenancyLicenceDetails.getTypeOfTenancyLicence();
        LocalDate tenancyDate = tenancyLicenceDetails.getTenancyLicenceDate();
        VerticalYesNo hasTenancyLicence = tenancyLicenceDetails.getHasCopyOfTenancyLicence();

        return TenancyLicenceTabDetails.builder()
            .typeOfTenancyLicence(tenancyType != null ? tenancyType.getLabel() : NO_ANSWER)
            .tenancyLicenceDate(tenancyDate != null ? tenancyDate.format(PATTERN) : NO_ANSWER)
            .hasCopyOfTenancyLicence(hasTenancyLicence != null ? hasTenancyLicence.getLabel() : NO_ANSWER)
            .tenancyLicenceDocuments(tenancyLicenceDetails.getTenancyLicenceDocuments())
            .reasonsForNoTenancyLicenceDocuments(tenancyLicenceDetails.getReasonsForNoTenancyLicenceDocuments())
            .build();
    }

    private NoticeTabDetails buildNoticeTabDetails(PCSCase pcsCase) {
        if (pcsCase.getNoticeServed() == null) {
            return NoticeTabDetails.builder()
                .noticeServed(NO_ANSWER)
                .noticeMethod(NO_ANSWER)
                .noticeDate(NO_ANSWER)
                .build();
        }

        YesOrNo noticeServed = pcsCase.getNoticeServed();
        NoticeTabDetails noticeTabDetails = NoticeTabDetails.builder()
            .noticeServed(noticeServed.getValue())
            .noticeMethod(NO_ANSWER)
            .noticeDate(NO_ANSWER)
            .build();

        NoticeServedDetails noticeServedDetails = pcsCase.getNoticeServedDetails();
        if (noticeServed == YesOrNo.YES && noticeTabDetails != null) {
            NoticeServiceMethod method = noticeServedDetails.getNoticeServiceMethod();
            noticeTabDetails.setNoticeDocuments(noticeServedDetails.getNoticeDocuments());

            if (method != null) {
                noticeTabDetails.setNoticeMethod(method.getLabel());
                switch (method) {
                    case FIRST_CLASS_POST -> {
                        LocalDate date = noticeServedDetails.getNoticePostedDate();
                        noticeTabDetails.setNoticeDate(date != null ? date.format(PATTERN) : NO_ANSWER);
                    }
                    case DELIVERED_PERMITTED_PLACE -> {
                        LocalDate date = noticeServedDetails.getNoticeDeliveredDate();
                        noticeTabDetails.setNoticeDate(date != null ? date.format(PATTERN) : NO_ANSWER);
                    }
                    case PERSONALLY_HANDED -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeHandedOverDateTime();
                        String name = noticeTabDetails.getNoticePersonName();
                        noticeTabDetails.setNoticeDate(dateTime != null ? dateTime.format(PATTERN) : NO_ANSWER);
                        noticeTabDetails.setNoticePersonName(name != null ? name : NO_ANSWER);
                    }
                    case EMAIL -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeEmailSentDateTime();
                        String emailAddress = noticeServedDetails.getNoticeEmailAddress();
                        noticeTabDetails.setNoticeDate( dateTime != null ? dateTime.format(PATTERN) : NO_ANSWER);
                        noticeTabDetails.setNoticeEmailAddress(emailAddress != null ? emailAddress : NO_ANSWER);
                    }
                    case OTHER_ELECTRONIC -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeOtherElectronicDateTime();
                        noticeTabDetails.setNoticeDate(dateTime != null ? dateTime.format(PATTERN) : NO_ANSWER);
                    }
                    case OTHER -> {
                        LocalDateTime dateTime = noticeServedDetails.getNoticeOtherDateTime();
                        String explanation = noticeTabDetails.getNoticeOtherExplanation();
                        noticeTabDetails.setNoticeDate(dateTime != null ? dateTime.format(PATTERN) : NO_ANSWER);
                        noticeTabDetails.setNoticeOtherExplanation(explanation != null ? explanation : NO_ANSWER);
                    }
                };
            }
        }

        return noticeTabDetails;
    }

    private ActionsTakenTabDetails buildActionsTakenTabDetails(PCSCase pcsCase) {
        VerticalYesNo preactionProtocol = pcsCase.getPreActionProtocolCompleted();
        VerticalYesNo mediationAttempted = pcsCase.getMediationAttempted();
        VerticalYesNo settlementAttempted = pcsCase.getSettlementAttempted();

        return ActionsTakenTabDetails.builder()
            .preactionProtocolFollowed(preactionProtocol != null ? preactionProtocol.getLabel() :NO_ANSWER)
            .mediationAttempted(mediationAttempted != null ? mediationAttempted.getLabel() : NO_ANSWER)
            .settlementAttempted(settlementAttempted != null ? settlementAttempted.getLabel() : NO_ANSWER)
            .build();
    }

    private RentArrearsTabDetails buildRentArrearsTabDetails(PCSCase pcsCase) {
        if (pcsCase.getShowRentSectionPage() != YesOrNo.YES) {
            return null;
        }

        RentArrearsTabDetails rentArrearsTabDetails = rentArrearsTabDetailsBuilder.buildRentArrearsTabDetails(pcsCase);

        if (rentArrearsTabDetails == null) {
            return RentArrearsTabDetails.builder()
                .rentAmount(NO_ANSWER)
                .calculationFrequency(NO_ANSWER)
                .frequency(NO_ANSWER)
                .dailyRate(NO_ANSWER)
                .stepsToRecoverArrears(NO_ANSWER)
                .arrearsTotal(NO_ANSWER)
                .judgmentRequested(NO_ANSWER)
                .build();
        }

        return rentArrearsTabDetails;
    }

    private CostsTabDetails buildCostsTabDetails(PCSCase pcsCase) {
        VerticalYesNo claimingCostsWanted = pcsCase.getClaimingCostsWanted();
        return CostsTabDetails.builder()
            .askingForCosts(claimingCostsWanted != null ? claimingCostsWanted.getLabel() : NO_ANSWER)
            .build();
    }

    private ReasonsForPossessionTabDetails buildReasonsForPossession(PCSCase pcsCase) {
        return reasonsForPossessionTabDetailsBuilder.buildReasonsForPossessionFromGroundSummaries(pcsCase);
    }

    private ApplicationsTabDetails buildApplicationsTabDetails(PCSCase pcsCase) {
        VerticalYesNo applicationWithClaim = pcsCase.getApplicationWithClaim();
        String planToMakeGeneralApplication = applicationWithClaim != null ?
            applicationWithClaim.getLabel() : NO_ANSWER;

        return ApplicationsTabDetails.builder()
            .planToMakeGeneralApplication(planToMakeGeneralApplication)
            .build();
    }

    private ClaimantInformationTabDetails buildClaimantInformationTabDetails(PCSCase pcsCase) {
        return claimantInformationTabDetailsBuilder.createSummaryClaimantTabDetails(pcsCase);
    }

    private AddressUK getClaimantAddress(PCSCase pcsCase) {
        List<ListValue<Party>> claimants = pcsCase.getAllClaimants();
        ClaimantContactPreferences claimantContactPreferences = pcsCase.getClaimantContactPreferences();
        AddressUK address = null;

        if (!CollectionUtils.isEmpty(claimants)) {
            Party claimant = claimants.getFirst().getValue();
            address = claimant.getAddress();
        } else if (claimantContactPreferences != null ) {
            YesOrNo orgAddressFound = claimantContactPreferences.getOrgAddressFound();
            VerticalYesNo correctClaimantAddress = claimantContactPreferences.getIsCorrectClaimantContactAddress();
            if (orgAddressFound == YesOrNo.YES && correctClaimantAddress == VerticalYesNo.YES) {
                address = claimantContactPreferences.getOrganisationAddress();
            } else {
                address = claimantContactPreferences.getOverriddenClaimantContactAddress();
            }
        }

        if (address != null) {
            return address;
        }

        return AddressUK.builder()
            .addressLine1(NO_ANSWER)
            .postTown(NO_ANSWER)
            .postCode(NO_ANSWER)
            .country(NO_ANSWER)
            .build();
    }

    private ClaimantContactTabDetails buildClaimantContactTabDetails(PCSCase pcsCase) {
        List<ListValue<Party>> claimants = pcsCase.getAllClaimants();
        ClaimantContactPreferences claimantContactPreferences = pcsCase.getClaimantContactPreferences();
        String emailAddress = null;
        String phoneNumber = null;

        if (!CollectionUtils.isEmpty(claimants)) {
            Party claimant = claimants.getFirst().getValue();
            emailAddress = claimant.getEmailAddress();
            phoneNumber = claimant.getPhoneNumber();
        } else if (claimantContactPreferences != null) {
            VerticalYesNo isCorrectClaimantContactEmail = claimantContactPreferences.getIsCorrectClaimantContactEmail();
            emailAddress = isCorrectClaimantContactEmail == VerticalYesNo.YES ?
                claimantContactPreferences.getClaimantContactEmail() :
                claimantContactPreferences.getOverriddenClaimantContactEmail();
            if (claimantContactPreferences.getClaimantProvidePhoneNumber() == VerticalYesNo.YES) {
                phoneNumber = claimantContactPreferences.getClaimantContactPhoneNumber();
            }
        }


        return ClaimantContactTabDetails.builder()
            .emailAddress(emailAddress != null ? emailAddress : NO_ANSWER)
            .phoneNumber(phoneNumber != null ? phoneNumber : NO_ANSWER)
            .build();
    }

    private DefendantCircumstanceTabDetails buildDefendantCircumstanceTabDetails(PCSCase pcsCase) {
        DefendantCircumstances defendantCircumstances = pcsCase.getDefendantCircumstances();
        String circumstances = null;
        VerticalYesNo circumstancesGiven = null;

        if (defendantCircumstances != null) {
            circumstancesGiven = defendantCircumstances.getHasDefendantCircumstancesInfo();
            circumstances = defendantCircumstances.getDefendantCircumstancesInfo();
        }


        return DefendantCircumstanceTabDetails.builder()
            .defendantCircumstancesGiven(circumstancesGiven != null ? circumstancesGiven.getLabel() : NO_ANSWER)
            .defendantCircumstances(circumstances)
            .build();
    }

    private List<ListValue<UnderlesseeOrMortgageInformationTabDetails>> buildUnderlesseeMortgageTabDetailsList(
        PCSCase pcsCase
    ) {
        List<ListValue<Party>> underlesseeMortgageParties = pcsCase.getAllUnderlesseeOrMortgagees();
        if (CollectionUtils.isEmpty(underlesseeMortgageParties)) {
            return null;
        }

        return underlesseeMortgageParties.stream()
            .map(this::buildUnderlesseeMortgageTabDetails)
            .toList();
    }

    private ListValue<UnderlesseeOrMortgageInformationTabDetails> buildUnderlesseeMortgageTabDetails(
        ListValue<Party> underlesseeMortgageePartyListValue
    ) {
        Party underlesseeMortgageeParty = underlesseeMortgageePartyListValue.getValue();
        VerticalYesNo nameKnown = underlesseeMortgageeParty.getNameKnown();
        String name = nameKnown == VerticalYesNo.YES ? underlesseeMortgageeParty.getOrgName() : null;
        VerticalYesNo addressKnown = underlesseeMortgageeParty.getAddressKnown();
        AddressUK address = addressKnown == VerticalYesNo.YES ? underlesseeMortgageeParty.getAddress() : null;

        return ListValue.<UnderlesseeOrMortgageInformationTabDetails>builder()
            .value(UnderlesseeOrMortgageInformationTabDetails.builder()
                       .nameKnown(nameKnown.getLabel())
                       .name(name)
                       .addressKnown(addressKnown.getLabel())
                       .address(address)
                       .build())
            .build();
    }

    private DemotionOfTenancyTabDetails buildDemotionOfTenancyTabDetails(PCSCase pcsCase) {
        Set<AlternativesToPossession> alternativesToPossessionSet = pcsCase.getAlternativesToPossession();

        if (
            CollectionUtils.isEmpty(alternativesToPossessionSet) ||
            !alternativesToPossessionSet.contains(DEMOTION_OF_TENANCY)
        ) {
            return null;
        }

        DemotionOfTenancy demotionOfTenancy = pcsCase.getDemotionOfTenancy();
        if (demotionOfTenancy == null) {
            return DemotionOfTenancyTabDetails.builder()
                .housingAct(NO_ANSWER)
                .statementOfExpressTermsServed(NO_ANSWER)
                .reasons(NO_ANSWER)
                .build();
        }

        DemotionOfTenancyHousingAct housingAct = demotionOfTenancy.getHousingAct();
        VerticalYesNo statementOfExpressTermsServed = demotionOfTenancy.getStatementOfExpressTermsServed();
        String statementOfExpressTermsDetails = null;

        if (statementOfExpressTermsServed == VerticalYesNo.YES) {
            statementOfExpressTermsDetails = demotionOfTenancy.getStatementOfExpressTermsDetails();
        }
        String reason = demotionOfTenancy.getReason();

        return DemotionOfTenancyTabDetails.builder()
            .housingAct(housingAct != null ? housingAct.getLabel() : NO_ANSWER)
            .statementOfExpressTermsServed(
                statementOfExpressTermsServed != null ? statementOfExpressTermsServed.getLabel() : NO_ANSWER
            )
            .terms(statementOfExpressTermsDetails)
            .reasons(reason != null ? reason : NO_ANSWER)
            .build();
    }

    private SuspensionOfRightToBuyTabDetails buildSuspensionOfRightToBuyTabDetails(PCSCase pcsCase) {
        Set<AlternativesToPossession> alternativesToPossessionSet = pcsCase.getAlternativesToPossession();
        if (
            CollectionUtils.isEmpty(alternativesToPossessionSet) ||
            !alternativesToPossessionSet.contains(SUSPENSION_OF_RIGHT_TO_BUY)
        ) {
            return null;
        }

        SuspensionOfRightToBuy suspensionOfRightToBuyDemotionOfTenancy = pcsCase.getSuspensionOfRightToBuy();
        if (suspensionOfRightToBuyDemotionOfTenancy == null) {
            return SuspensionOfRightToBuyTabDetails.builder()
                .housingAct(NO_ANSWER)
                .reasons(NO_ANSWER)
                .build();
        }

        SuspensionOfRightToBuyHousingAct housingAct = suspensionOfRightToBuyDemotionOfTenancy.getHousingAct();
        String reason = suspensionOfRightToBuyDemotionOfTenancy.getReason();

        return SuspensionOfRightToBuyTabDetails.builder()
            .housingAct(housingAct != null ? housingAct.getLabel() : NO_ANSWER)
            .reasons(reason != null ? reason : NO_ANSWER)
            .build();
    }
}
