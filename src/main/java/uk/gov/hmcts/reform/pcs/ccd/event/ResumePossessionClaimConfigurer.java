package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.common.PageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.makeaclaim.StatementOfTruth;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AdditionalReasonsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AlternativesToPossessionOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.AssuredNoArrearsGroundsForPossessionPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantInformationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantTypeNotEligibleEngland;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ClaimantTypeNotEligibleWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.CompletingYourClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DailyRentAmount;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantCircumstancesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DefendantsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DemotionOfTenancyHousingActOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.DemotionOfTenancyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.GroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOrOtherGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.IntroductoryDemotedOtherGroundsReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.MediationAndSettlement;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.MoneyJudgment;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoRentArrearsGroundsForPossessionReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.NoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.PreActionProtocol;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrears;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundForPossessionAdditionalGrounds;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossessionPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentArrearsOrBreachOfTenancyGround;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.RentDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.ResumeClaim;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossession;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SecureOrFlexibleGroundsForPossessionReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SelectClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.StatementOfExpressTerms;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyHousingActOptions;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionOfRightToBuyOrderReason;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionToBuyDemotionOfTenancyActs;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.SuspensionToBuyDemotionOfTenancyOrderReasons;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.TenancyLicenceDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeOrMortgageeDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UnderlesseeOrMortgageeEntitledToClaimRelief;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.UploadAdditionalDocumentsDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WalesCheckingNotice;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.WantToUploadDocuments;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ASBQuestionsWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.GroundsForPossessionWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.OccupationLicenceDetailsWalesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ProhibitedConductWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.ReasonsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim.wales.SecureContractGroundsForPossessionWalesPage;

@Component
@AllArgsConstructor
public class ResumePossessionClaimConfigurer implements PageConfigurer {

    private final ResumeClaim resumeClaim;
    private final SelectClaimantType selectClaimantType;
    private final NoticeDetails noticeDetails;
    private final UploadAdditionalDocumentsDetails uploadAdditionalDocumentsDetails;
    private final TenancyLicenceDetailsPage tenancyLicenceDetails;
    private final ContactPreferences contactPreferences;
    private final DefendantsDetails defendantsDetails;
    private final NoRentArrearsGroundsForPossessionReason noRentArrearsGroundsForPossessionReason;
    private final AdditionalReasonsForPossession additionalReasonsForPossession;
    private final SecureOrFlexibleGroundsForPossessionReasons secureOrFlexibleGroundsForPossessionReasons;
    private final MediationAndSettlement mediationAndSettlement;
    private final ClaimantCircumstancesPage claimantCircumstancesPage;
    private final IntroductoryDemotedOtherGroundsReasons introductoryDemotedOtherGroundsReasons;
    private final IntroductoryDemotedOrOtherGroundsForPossession introductoryDemotedOrOtherGroundsForPossession;
    private final RentArrearsGroundsForPossessionReasons rentArrearsGroundsForPossessionReasons;
    private final SuspensionToBuyDemotionOfTenancyOrderReasons suspensionToBuyDemotionOfTenancyOrderReasons;
    private final DefendantCircumstancesPage defendantCircumstancesPage;
    private final SuspensionOfRightToBuyOrderReason suspensionOfRightToBuyOrderReason;
    private final StatementOfExpressTerms statementOfExpressTerms;
    private final DemotionOfTenancyOrderReason demotionOfTenancyOrderReason;
    private final ClaimantInformationPage claimantInformationPage;
    private final ClaimantDetailsWalesPage claimantDetailsWales;
    private final ProhibitedConductWales prohibitedConductWalesPage;
    private final OccupationLicenceDetailsWalesPage occupationLicenceDetailsWalesPage;
    private final GroundsForPossessionWalesPage groundsForPossessionWales;
    private final SecureContractGroundsForPossessionWalesPage secureContractGroundsForPossessionWales;
    private final ReasonsForPossessionWales reasonsForPossessionWales;
    private final RentArrearsGroundsForPossessionPage rentArrearsGroundsForPossessionPage;
    private final RentArrearsGroundForPossessionAdditionalGrounds rentArrearsGroundForPossessionAdditionalGrounds;
    private final AssuredNoArrearsGroundsForPossessionPage noRentArrearsGroundsForPossessionOptions;
    private final CheckingNotice checkingNotice;
    private final WalesCheckingNotice walesCheckingNotice;
    private final ASBQuestionsWales asbQuestionsWales;
    private final UnderlesseeOrMortgageeDetailsPage underlesseeOrMortgageeDetailsPage;
    private final RentDetailsPage rentDetailsPage;

    @Override
    public void configurePages(PageBuilder pageBuilder) {
        pageBuilder.add(resumeClaim)
            .add(selectClaimantType)
            .add(new ClaimantTypeNotEligibleEngland())
            .add(new ClaimantTypeNotEligibleWales())
            .add(new SelectClaimType())
            .add(new ClaimTypeNotEligibleEngland())
            .add(new ClaimTypeNotEligibleWales())
            .add(claimantInformationPage)
            .add(claimantDetailsWales)
            .add(contactPreferences)
            .add(defendantsDetails)
            .add(tenancyLicenceDetails)
            .add(occupationLicenceDetailsWalesPage)
            .add(groundsForPossessionWales)
            .add(secureContractGroundsForPossessionWales)
            .add(reasonsForPossessionWales)
            .add(asbQuestionsWales)
            .add(new SecureOrFlexibleGroundsForPossession())
            .add(new RentArrearsOrBreachOfTenancyGround())
            .add(secureOrFlexibleGroundsForPossessionReasons)
            .add(introductoryDemotedOrOtherGroundsForPossession)
            .add(introductoryDemotedOtherGroundsReasons)
            .add(new GroundsForPossession())
            .add(rentArrearsGroundsForPossessionPage)
            .add(rentArrearsGroundForPossessionAdditionalGrounds)
            .add(rentArrearsGroundsForPossessionReasons)
            .add(noRentArrearsGroundsForPossessionOptions)
            .add(noRentArrearsGroundsForPossessionReason)
            .add(new PreActionProtocol())
            .add(mediationAndSettlement)
            .add(checkingNotice)
            .add(walesCheckingNotice)
            .add(noticeDetails)
            .add(rentDetailsPage)
            .add(new DailyRentAmount())
            .add(new RentArrears())
            .add(new MoneyJudgment())
            .add(claimantCircumstancesPage)
            .add(defendantCircumstancesPage)
            .add(prohibitedConductWalesPage)
            .add(new AlternativesToPossessionOptions())
            .add(new SuspensionOfRightToBuyHousingActOptions())
            .add(suspensionOfRightToBuyOrderReason)
            .add(new DemotionOfTenancyHousingActOptions())
            .add(new SuspensionToBuyDemotionOfTenancyActs())
            .add(statementOfExpressTerms)
            .add(demotionOfTenancyOrderReason)
            .add(suspensionToBuyDemotionOfTenancyOrderReasons)
            .add(additionalReasonsForPossession)
            .add(new UnderlesseeOrMortgageeEntitledToClaimRelief())
            .add(underlesseeOrMortgageeDetailsPage)
            //TO DO will be routed later on  correctly using tech debt ticket
            .add(new WantToUploadDocuments())
            .add(uploadAdditionalDocumentsDetails)
            .add(new GeneralApplication())
            .add(new LanguageUsed())
            .add(new CompletingYourClaim())
            .add(new StatementOfTruth());
    }

}
