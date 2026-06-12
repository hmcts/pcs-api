package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalOtherGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.IntroductoryDemotedOrOtherGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ActionsTakenTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.AntisocialAndConductTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ApplicationsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ClaimTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ClaimantCircumstancesTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ClaimantContactTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ClaimantRegistrationAndLicensingTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.DefendantCircumstanceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.DemotionOfTenancyTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.ProhibitedConductStandardContractTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.SuspensionOfRightToBuyTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.TenancyLicenceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.UnderlesseeOrMortgageInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.AdditionalDefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ClaimantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.DefendantInformationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.GroundsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.OccupationContractOrLicenceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ReasonsForPossessionTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.ASBQuestionsDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.PeriodicContractTermsWales;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.AdditionalDefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.DefendantInformationTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.GroundsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ReasonsForPossessionTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.RequiredDocumentsTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.RentArrearsTabDetailsBuilder;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.ASSURED_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.DEMOTED_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.INTRODUCTORY_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType.OTHER;
import static uk.gov.hmcts.reform.pcs.config.ClockConfiguration.UK_ZONE_ID;

@AllArgsConstructor
@Component
public class CaseDetailsTabView {

    private static final String NO_ANSWER = " ";
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm:ssa", Locale.UK);

    private final GroundsBuilder groundsBuilder;
    private final RentArrearsTabDetailsBuilder rentArrearsTabDetailsBuilder;
    private final ReasonsForPossessionTabDetailsBuilder reasonsForPossessionTabDetailsBuilder;
    private final ClaimantInformationTabDetailsBuilder claimantInformationTabDetailsBuilder;
    private final DefendantInformationTabDetailsBuilder defendantInformationTabDetailsBuilder;
    private final AdditionalDefendantInformationTabDetailsBuilder additionalDefendantInformationTabDetailsBuilder;
    private final RequiredDocumentsTabDetailsBuilder requiredDocumentsTabDetailsBuilder;

    public CaseDetailsTab buildCaseDetailsTab(PCSCase pcsCase) {
        ClaimTabDetails claimTabDetails = buildClaimTabDetails(pcsCase);
        GroundsForPossessionTabDetails groundsForPossessionTabDetails = buildGroundsForPossessionTabDetails(pcsCase);
        TenancyLicenceTabDetails tenancyLicenceTabDetails = buildTenancyLicenceTabDetails(pcsCase);
        NoticeTabDetails noticeTabDetails = buildNoticeTabDetails(pcsCase);
        ActionsTakenTabDetails actionsTakenTabDetails = buildActionsTakenTabDetails(pcsCase);
        RentArrearsTabDetails rentArrearsTabDetails =
            rentArrearsTabDetailsBuilder.buildDetailedRentArrearsTabDetails(pcsCase);
        ReasonsForPossessionTabDetails reasonsForPossessionTabDetails =
            reasonsForPossessionTabDetailsBuilder.buildDetailsReasonsForPossession(pcsCase);
        ApplicationsTabDetails applicationsTabDetails = buildApplicationsTabDetails(pcsCase);
        ClaimantInformationTabDetails claimantInformationTabDetails = buildClaimantInformationTabDetails(pcsCase);
        DefendantInformationTabDetails defendantInformationTabDetails =
            defendantInformationTabDetailsBuilder.buildDetailedDefendantDetails(pcsCase);
        UnderlesseeOrMortgageInformationTabDetails underlesseeMortgageOneTabDetails =
            buildUnderlesseeOrMortgageOneInformationTabDetails(pcsCase);
        DemotionOfTenancyTabDetails demotionOfTenancyTabDetails = buildDemotionOfTenancyTabDetails(pcsCase);
        SuspensionOfRightToBuyTabDetails suspensionOfRightToBuyTabDetails =
            buildSuspensionOfRightToBuyTabDetails(pcsCase);
        String dateSubmitted = formatSubmittedDate(pcsCase.getDateSubmitted());
        OccupationContractOrLicenceTabDetails occupationContractLicenceTabDetails =
            buildOccupationContractLicenceTabDetails(pcsCase);
        AntisocialAndConductTabDetails antisocialAndConductTabDetails = buildAntisocialAndConductTabDetails(pcsCase);
        ProhibitedConductStandardContractTabDetails prohibitedConductStandardContractTabDetails =
            buildProhibitedConductStandardContractTabDetails(pcsCase);

        CaseDetailsTab caseDetailsTab = CaseDetailsTab.builder()
            .claimDetails(claimTabDetails)
            .propertyAddress(pcsCase.getPropertyAddress())
            .groundsForPossessionDetails(groundsForPossessionTabDetails)
            .tenancyLicenceDetails(tenancyLicenceTabDetails)
            .noticeDetails(noticeTabDetails)
            .actionsTakenDetails(actionsTakenTabDetails)
            .rentArrearsDetails(rentArrearsTabDetails)
            .reasonsForPossessionDetails(reasonsForPossessionTabDetails)
            .applicationsDetails(applicationsTabDetails)
            .claimantInformation(claimantInformationTabDetails)
            .defendantInformationDetails(defendantInformationTabDetails)
            .mortgageOneDetails(underlesseeMortgageOneTabDetails)
            .demotionOfTenancyDetails(demotionOfTenancyTabDetails)
            .suspensionOfRightToBuyDetails(suspensionOfRightToBuyTabDetails)
            .dateClaimSubmitted(dateSubmitted != null ? dateSubmitted : NO_ANSWER)
            .occupationContractLicenceDetails(occupationContractLicenceTabDetails)
            .antisocialAndConductDetails(antisocialAndConductTabDetails)
            .prohibitedConductStandardContractDetails(prohibitedConductStandardContractTabDetails)
            .requiredDocumentsDetails(requiredDocumentsTabDetailsBuilder.buildRequiredDocumentsTabDetails(pcsCase))
            .build();

        if (claimantInformationTabDetails != null) {
            caseDetailsTab.setClaimantAddress(getClaimantAddress(pcsCase));
            caseDetailsTab.setClaimantContactDetails(buildClaimantContactTabDetails(pcsCase));
            caseDetailsTab.setClaimantCircumstances(buildClaimantCircumstancesTabDetails(pcsCase));
            caseDetailsTab
                .setClaimantRegistrationAndLicensingDetails(buildClaimantRegistrationAndLicensingTabDetails(pcsCase));
        }

        if (defendantInformationTabDetails != null) {
            List<ListValue<AdditionalDefendantInformationTabDetails>> additionalDefendantInformationTabDetails =
                additionalDefendantInformationTabDetailsBuilder.buildDetailedAdditionalDefendantsDetails(pcsCase);
            caseDetailsTab.setAdditionalDefendants(additionalDefendantInformationTabDetails);
            caseDetailsTab.setDefendantCircumstanceDetails(buildDefendantCircumstanceTabDetails(pcsCase));
        }

        if (underlesseeMortgageOneTabDetails != null) {
            List<ListValue<UnderlesseeOrMortgageInformationTabDetails>> underlesseeMortgageTabDetailsList =
                buildUnderlesseeMortgageTabDetailsList(pcsCase);
            caseDetailsTab.setMortgageDetails(underlesseeMortgageTabDetailsList);
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
        TenancyLicenceType tenancyType = tenancyLicenceDetails != null
            ? tenancyLicenceDetails.getTypeOfTenancyLicence() : null;
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
        if (pcsCase.getLegislativeCountry() != LegislativeCountry.ENGLAND) {
            return null;
        }

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
            .tenancyLicenceDescription(
                tenancyType == OTHER ? tenancyLicenceDetails.getDetailsOfOtherTypeOfTenancyLicence() : null
            )
            .tenancyLicenceDate(tenancyDate != null ? tenancyDate.format(DATE_FORMATTER) : NO_ANSWER)
            .hasCopyOfTenancyLicence(hasTenancyLicence != null ? hasTenancyLicence.getLabel() : NO_ANSWER)
            .tenancyLicenceDocuments(tenancyLicenceDetails.getTenancyLicenceDocuments())
            .reasonsForNoTenancyLicenceDocuments(tenancyLicenceDetails.getReasonsForNoTenancyLicenceDocuments())
            .build();
    }

    private NoticeTabDetails buildNoticeTabDetails(PCSCase pcsCase) {
        if (pcsCase.getLegislativeCountry() == LegislativeCountry.WALES) {
            return buildNoticeTabDetailsWales(pcsCase);
        }

        return buildNoticeTabDetailsEngland(pcsCase);
    }

    private NoticeTabDetails buildNoticeTabDetailsEngland(PCSCase pcsCase) {
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


        setNoticeServedDetails(noticeServed, noticeTabDetails, pcsCase);

        return noticeTabDetails;
    }

    private NoticeTabDetails buildNoticeTabDetailsWales(PCSCase pcsCase) {
        WalesNoticeDetails walesNoticeDetails = pcsCase.getWalesNoticeDetails();

        if (walesNoticeDetails == null) {
            return NoticeTabDetails.builder()
                .noticeServed(NO_ANSWER)
                .noticeMethod(NO_ANSWER)
                .noticeDate(NO_ANSWER)
                .build();
        }

        YesOrNo noticeServed = walesNoticeDetails.getNoticeServed();

        NoticeTabDetails noticeTabDetails = NoticeTabDetails.builder()
            .noticeServed(noticeServed != null ? noticeServed.getValue() : NO_ANSWER)
            .typeOfNoticeServed(noticeServed == YesOrNo.YES ? walesNoticeDetails.getTypeOfNoticeServed() : null)
            .statement(noticeServed == YesOrNo.NO ? walesNoticeDetails.getNoticeStatement() : null)
            .noticeMethod(NO_ANSWER)
            .noticeDate(NO_ANSWER)
            .build();

        setNoticeServedDetails(noticeServed, noticeTabDetails, pcsCase);

        return noticeTabDetails;
    }

    private void setNoticeServedDetails(
        YesOrNo noticeServed,
        NoticeTabDetails noticeTabDetails,
        PCSCase pcsCase
    ) {
        NoticeServedDetails noticeServedDetails = pcsCase.getNoticeServedDetails();

        if (noticeServed != YesOrNo.YES || noticeServedDetails == null) {
            return;
        }

        NoticeServiceMethod method = noticeServedDetails.getNoticeServiceMethod();
        noticeTabDetails.setNoticeDocuments(noticeServedDetails.getNoticeDocuments());

        if (method != null) {
            noticeTabDetails.setNoticeMethod(method.getLabel());
            switch (method) {
                case FIRST_CLASS_POST -> {
                    LocalDate date = noticeServedDetails.getNoticePostedDate();
                    noticeTabDetails.setNoticeDate(date != null ? date.format(DATE_FORMATTER) : NO_ANSWER);
                }
                case DELIVERED_PERMITTED_PLACE -> {
                    LocalDate date = noticeServedDetails.getNoticeDeliveredDate();
                    noticeTabDetails.setNoticeDate(date != null ? date.format(DATE_FORMATTER) : NO_ANSWER);
                }
                case PERSONALLY_HANDED -> {
                    LocalDateTime dateTime = noticeServedDetails.getNoticeHandedOverDateTime();
                    String name = noticeServedDetails.getNoticePersonName();
                    noticeTabDetails.setNoticeDate(dateTime != null ? formatDateTime(dateTime) : NO_ANSWER);
                    noticeTabDetails.setNoticePersonName(name != null ? name : NO_ANSWER);
                }
                case EMAIL -> {
                    LocalDateTime dateTime = noticeServedDetails.getNoticeEmailSentDateTime();
                    String emailAddress = noticeServedDetails.getNoticeEmailAddress();
                    noticeTabDetails.setNoticeDate(dateTime != null ? formatDateTime(dateTime) : NO_ANSWER);
                    noticeTabDetails.setNoticeEmailAddress(emailAddress != null ? emailAddress : NO_ANSWER);
                }
                case OTHER_ELECTRONIC -> {
                    LocalDateTime dateTime = noticeServedDetails.getNoticeOtherElectronicDateTime();
                    String details = noticeServedDetails.getNoticeOtherElectronicMethodExplanation();
                    noticeTabDetails.setNoticeDate(dateTime != null ? formatDateTime(dateTime) : NO_ANSWER);
                    noticeTabDetails.setNoticeOtherElectronicDetails(details != null ? details : NO_ANSWER);
                }
                case OTHER -> {
                    LocalDateTime dateTime = noticeServedDetails.getNoticeOtherDateTime();
                    String explanation = noticeServedDetails.getNoticeOtherExplanation();
                    noticeTabDetails.setNoticeDate(dateTime != null ? formatDateTime(dateTime) : NO_ANSWER);
                    noticeTabDetails.setNoticeOtherExplanation(explanation != null ? explanation : NO_ANSWER);
                }
            }
        }
    }

    private ActionsTakenTabDetails buildActionsTakenTabDetails(PCSCase pcsCase) {
        VerticalYesNo preactionProtocol = pcsCase.getPreActionProtocolCompleted();
        VerticalYesNo mediationAttempted = pcsCase.getMediationAttempted();
        VerticalYesNo settlementAttempted = pcsCase.getSettlementAttempted();
        String noPreactionExplanation = null;

        if (preactionProtocol == VerticalYesNo.NO) {
            noPreactionExplanation = pcsCase.getPreActionProtocolIncompleteExplanation();
        }

        return ActionsTakenTabDetails.builder()
            .preactionProtocolFollowed(preactionProtocol != null ? preactionProtocol.getLabel() : NO_ANSWER)
            .preActionProtocolIncompleteExplanation(noPreactionExplanation)
            .mediationAttempted(mediationAttempted != null ? mediationAttempted.getLabel() : NO_ANSWER)
            .settlementAttempted(settlementAttempted != null ? settlementAttempted.getLabel() : NO_ANSWER)
            .build();
    }

    private ApplicationsTabDetails buildApplicationsTabDetails(PCSCase pcsCase) {
        VerticalYesNo applicationWithClaim = pcsCase.getApplicationWithClaim();
        String planToMakeGeneralApplication = applicationWithClaim != null
            ? applicationWithClaim.getLabel() : NO_ANSWER;

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
        } else if (claimantContactPreferences != null) {
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
        VerticalYesNo phoneNumberProvided = null;

        if (!CollectionUtils.isEmpty(claimants)) {
            Party claimant = claimants.getFirst().getValue();
            emailAddress = claimant.getEmailAddress();
            phoneNumberProvided = claimant.getPhoneNumberProvided();

            if (phoneNumberProvided == VerticalYesNo.YES) {
                phoneNumber = claimant.getPhoneNumber();
            }
        } else if (claimantContactPreferences != null) {
            VerticalYesNo isCorrectClaimantContactEmail = claimantContactPreferences.getIsCorrectClaimantContactEmail();
            emailAddress = isCorrectClaimantContactEmail == VerticalYesNo.YES
                ? claimantContactPreferences.getClaimantContactEmail() :
                claimantContactPreferences.getOverriddenClaimantContactEmail();
            phoneNumberProvided = claimantContactPreferences.getClaimantProvidePhoneNumber();
            if (phoneNumberProvided == VerticalYesNo.YES) {
                phoneNumber = claimantContactPreferences.getClaimantContactPhoneNumber();
            }
        }


        return ClaimantContactTabDetails.builder()
            .emailAddress(emailAddress != null ? emailAddress : NO_ANSWER)
            .phoneNumberProvided(phoneNumberProvided != null ? phoneNumberProvided.getLabel() : NO_ANSWER)
            .phoneNumber(phoneNumber)
            .build();
    }

    private ClaimantCircumstancesTabDetails buildClaimantCircumstancesTabDetails(PCSCase pcsCase) {
        ClaimantCircumstances claimantCircumstances = pcsCase.getClaimantCircumstances();

        if (claimantCircumstances == null) {
            return ClaimantCircumstancesTabDetails.builder()
                .claimantCircumstancesGiven(NO_ANSWER)
                .build();
        }
        VerticalYesNo claimantCircumstancesGiven = claimantCircumstances.getClaimantCircumstancesSelect();
        String claimantCircumstancesDetails = null;

        if (claimantCircumstancesGiven == VerticalYesNo.YES) {
            claimantCircumstancesDetails = claimantCircumstances.getClaimantCircumstancesDetails();
        }

        return ClaimantCircumstancesTabDetails.builder()
            .claimantCircumstancesGiven(
                claimantCircumstancesGiven != null ? claimantCircumstancesGiven.getLabel() : NO_ANSWER
            )
            .claimantCircumstancesDetails(claimantCircumstancesDetails)
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

    private UnderlesseeOrMortgageInformationTabDetails buildUnderlesseeOrMortgageOneInformationTabDetails(
        PCSCase pcsCase
    ) {
        List<ListValue<Party>> underlesseeMortgageParties = pcsCase.getAllUnderlesseeOrMortgagees();
        if (CollectionUtils.isEmpty(underlesseeMortgageParties)) {
            return null;
        }

        return buildUnderlesseeMortgageTabDetails(underlesseeMortgageParties.getFirst());
    }

    private List<ListValue<UnderlesseeOrMortgageInformationTabDetails>> buildUnderlesseeMortgageTabDetailsList(
        PCSCase pcsCase
    ) {
        List<ListValue<Party>> underlesseeMortgageParties = pcsCase.getAllUnderlesseeOrMortgagees();
        if (CollectionUtils.isEmpty(underlesseeMortgageParties) || underlesseeMortgageParties.size() < 2) {
            return List.of();
        }

        return underlesseeMortgageParties.stream()
            .skip(1)
            .map(this::buildUnderlesseeMortgageTabDetails)
            .map(details ->
                     ListValue.<UnderlesseeOrMortgageInformationTabDetails>builder().value(details).build()
            ).toList();
    }

    private UnderlesseeOrMortgageInformationTabDetails buildUnderlesseeMortgageTabDetails(
        ListValue<Party> underlesseeMortgageePartyListValue
    ) {
        Party underlesseeMortgageeParty = underlesseeMortgageePartyListValue.getValue();
        VerticalYesNo nameKnown = underlesseeMortgageeParty.getNameKnown();
        String name = nameKnown == VerticalYesNo.YES ? underlesseeMortgageeParty.getOrgName() : null;
        VerticalYesNo addressKnown = underlesseeMortgageeParty.getAddressKnown();
        AddressUK address = addressKnown == VerticalYesNo.YES ? underlesseeMortgageeParty.getAddress() : null;

        return UnderlesseeOrMortgageInformationTabDetails.builder()
            .nameKnown(nameKnown != null ? nameKnown.getLabel() : NO_ANSWER)
            .name(name)
            .addressKnown(addressKnown != null ? addressKnown.getLabel() : NO_ANSWER)
            .address(address)
            .build();
    }

    private DemotionOfTenancyTabDetails buildDemotionOfTenancyTabDetails(PCSCase pcsCase) {
        Set<AlternativesToPossession> alternativesToPossessionSet = pcsCase.getAlternativesToPossession();

        if (
            CollectionUtils.isEmpty(alternativesToPossessionSet)
            || !alternativesToPossessionSet.contains(DEMOTION_OF_TENANCY)
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
            CollectionUtils.isEmpty(alternativesToPossessionSet)
            || !alternativesToPossessionSet.contains(SUSPENSION_OF_RIGHT_TO_BUY)
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

    private String formatSubmittedDate(LocalDateTime dateSubmitted) {
        if (dateSubmitted == null) {
            return null;
        }

        LocalDateTime ukDateSubmitted = dateSubmitted
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(UK_ZONE_ID)
            .toLocalDateTime();

        return formatDateTime(ukDateSubmitted);
    }

    private String formatDateTime(LocalDateTime localDateTime) {
        return localDateTime.format(DATE_TIME_FORMATTER).replace("am", "AM").replace("pm", "PM");
    }

    private OccupationContractOrLicenceTabDetails buildOccupationContractLicenceTabDetails(PCSCase pcsCase) {
        if (pcsCase.getLegislativeCountry() != LegislativeCountry.WALES) {
            return null;
        }

        OccupationLicenceDetailsWales occupationLicenceDetailsWales = pcsCase.getOccupationLicenceDetailsWales();
        if (occupationLicenceDetailsWales == null) {
            return OccupationContractOrLicenceTabDetails.builder()
                .agreementType(NO_ANSWER)
                .agreementStartDate(NO_ANSWER)
                .documentsPlaceholder(NO_ANSWER)
                .build();
        }

        OccupationLicenceTypeWales agreementType = occupationLicenceDetailsWales.getOccupationLicenceTypeWales();
        LocalDate startDate = occupationLicenceDetailsWales.getLicenceStartDate();
        List<ListValue<Document>> documents = occupationLicenceDetailsWales.getLicenceDocuments();

        return OccupationContractOrLicenceTabDetails.builder()
            .agreementType(agreementType != null ? agreementType.getLabel() : NO_ANSWER)
            .agreementTypeDescription(
                agreementType == OccupationLicenceTypeWales.OTHER
                    ? occupationLicenceDetailsWales.getOtherLicenceTypeDetails() : null
            )
            .agreementStartDate(startDate != null ? startDate.format(DATE_FORMATTER) : NO_ANSWER)
            .documents(documents)
            .documentsPlaceholder(CollectionUtils.isEmpty(documents) ? NO_ANSWER : null)
            .build();
    }

    private ClaimantRegistrationAndLicensingTabDetails buildClaimantRegistrationAndLicensingTabDetails(
        PCSCase pcsCase
    ) {
        if (pcsCase.getLegislativeCountry() != LegislativeCountry.WALES) {
            return null;
        }

        VerticalYesNo isExemptLandlord = pcsCase.getIsExemptLandlord();

        return ClaimantRegistrationAndLicensingTabDetails.builder()
            .isExemptLandlord(isExemptLandlord != null ? isExemptLandlord.getLabel() : NO_ANSWER)
            .build();
    }

    private AntisocialAndConductTabDetails buildAntisocialAndConductTabDetails(PCSCase pcsCase) {
        if (
            pcsCase.getLegislativeCountry() != LegislativeCountry.WALES
                || pcsCase.getShowASBQuestionsPageWales() != YesOrNo.YES
        ) {
            return null;
        }

        ASBQuestionsDetailsWales asbQuestionsDetailsWales = pcsCase.getAsbQuestionsWales();
        if (asbQuestionsDetailsWales == null) {
            return AntisocialAndConductTabDetails.builder()
                .antiSocialBehaviour(NO_ANSWER)
                .propertyUsedIllegally(NO_ANSWER)
                .otherProhibitedConduct(NO_ANSWER)
                .build();
        }

        VerticalYesNo antiSocialBehaviour = asbQuestionsDetailsWales.getAntisocialBehaviour();
        VerticalYesNo propertyUsedIllegally = asbQuestionsDetailsWales.getIllegalPurposesUse();
        VerticalYesNo otherProhibitedConduct = asbQuestionsDetailsWales.getOtherProhibitedConduct();
        String antiSocialBehaviourDetails = null;
        String propertyUsedIllegallyDetails = null;
        String otherProhibitedConductDetails = null;

        if (antiSocialBehaviour == VerticalYesNo.YES) {
            antiSocialBehaviourDetails = asbQuestionsDetailsWales.getAntisocialBehaviourDetails();
        }

        if (propertyUsedIllegally == VerticalYesNo.YES) {
            propertyUsedIllegallyDetails = asbQuestionsDetailsWales.getIllegalPurposesUseDetails();
        }

        if (otherProhibitedConduct == VerticalYesNo.YES) {
            otherProhibitedConductDetails = asbQuestionsDetailsWales.getOtherProhibitedConductDetails();
        }

        return AntisocialAndConductTabDetails.builder()
            .antiSocialBehaviour(antiSocialBehaviour != null ? antiSocialBehaviour.getLabel() : NO_ANSWER)
            .propertyUsedIllegally(propertyUsedIllegally != null ? propertyUsedIllegally.getLabel() : NO_ANSWER)
            .otherProhibitedConduct(otherProhibitedConduct != null ? otherProhibitedConduct.getLabel() : NO_ANSWER)
            .antiSocialBehaviourDetails(antiSocialBehaviourDetails)
            .propertyUsedIllegallyDetails(propertyUsedIllegallyDetails)
            .otherProhibitedConductDetails(otherProhibitedConductDetails)
            .build();
    }

    private ProhibitedConductStandardContractTabDetails buildProhibitedConductStandardContractTabDetails(
        PCSCase pcsCase
    ) {
        VerticalYesNo prohibitedConduct = pcsCase.getProhibitedConductWalesClaim();
        if (pcsCase.getLegislativeCountry() != LegislativeCountry.WALES || prohibitedConduct == null) {
            return null;
        }

        ProhibitedConductStandardContractTabDetails prohibitedConductStandardContractTabDetails =
            ProhibitedConductStandardContractTabDetails.builder()
                .seekingProhibitedConductStandardContract(prohibitedConduct.getLabel())
                .build();

        PeriodicContractTermsWales periodicContractTermsWales = pcsCase.getPeriodicContractTermsWales();

        if (periodicContractTermsWales != null && prohibitedConduct == VerticalYesNo.YES) {
            String prohibitedConductWalesClaimDetails = pcsCase.getProhibitedConductWalesClaimDetails();
            prohibitedConductStandardContractTabDetails.setWhyMakingClaim(prohibitedConductWalesClaimDetails);

            VerticalYesNo agreedTerms = periodicContractTermsWales.getAgreedTermsOfPeriodicContract();
            prohibitedConductStandardContractTabDetails
                .setAgreedTerms(agreedTerms != null ? agreedTerms.getLabel() : NO_ANSWER);

            if (agreedTerms == VerticalYesNo.YES) {
                prohibitedConductStandardContractTabDetails
                    .setTermDetails(periodicContractTermsWales.getDetailsOfTerms());
            }
        }

        return prohibitedConductStandardContractTabDetails;
    }
}
