import { Page } from "@playwright/test";
import { initializeExecutor } from "@utils/controller";
import { performAction, performValidation } from "@utils/controller";
import { actionData, actionRecord, IAction } from "@utils/interfaces/action.interface";
import { home, addressDetails, additionalReasonsForPossession, alternativesToPossession, applications, checkYourAnswers, claimantCircumstances, claimantName,
         claimantType, claimingCosts, claimType, completeYourClaim, contactPreferences, dailyRentAmount, defendantCircumstances, defendantDetails,
         groundsForPossession, languageUsed, mediationAndSettlement, moneyJudgment, noticeDetails, noticeOfYourIntention, preActionProtocol,
         provideMoreDetailsOfClaim, rentArrearsPossessionGrounds, rentDetails, statementOfTruth, tenancyLicenceDetails, underlesseeOrMortgageeEntitledToClaim,
         wantToUploadDocuments, whatAreYourGroundsForPossession} from "@data/page-data";

export class MakeClaimAction implements IAction {
  async execute(page: Page, action: string, fieldName?: actionData | actionRecord, value?: actionData | actionRecord): Promise<void> {

    const actionsMap = new Map<string, () => Promise<void>>([
      ['createNewCase', () => this.createNewCase(page, fieldName as actionData)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async createNewCase(page: Page, criteria: actionData): Promise<void> {
    if (criteria == true) {
      initializeExecutor(page);
      await performAction('clickTab', home.createCaseTab);
      await performAction('selectJurisdictionCaseTypeEvent');
      await performAction('housingPossessionClaim');
      await performAction('selectAddress', {
        postcode: addressDetails.englandCourtAssignedPostcode,
        addressIndex: addressDetails.addressIndex
      });
      await performValidation('bannerAlert', 'Case #.* has been created.');
      await performAction('extractCaseIdFromAlert');
      await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
      await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
      await performAction('selectClaimType', claimType.no);
      await performAction('selectClaimantName', claimantName.yes);
      await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
      await performAction('selectContactPreferences', {
        notifications: contactPreferences.yes,
        correspondenceAddress: contactPreferences.yes,
        phoneNumber: contactPreferences.no
      });
      await performAction('defendantDetails', {
        name: defendantDetails.yes,
        correspondenceAddress: defendantDetails.yes,
        email: defendantDetails.yes,
        correspondenceAddressSame: defendantDetails.no
      });
      await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
      await performAction('selectTenancyOrLicenceDetails', {
        tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy,
        day: tenancyLicenceDetails.day,
        month: tenancyLicenceDetails.month,
        year: tenancyLicenceDetails.year,
        files: ['tenancyLicence.docx', 'tenancyLicence.png']
      });
      await performValidation('mainHeader', groundsForPossession.mainHeader);
      await performAction('selectGroundsForPossession', { groundsRadioInput: groundsForPossession.yes });
      await performAction('selectRentArrearsPossessionGround', {
        rentArrears: [rentArrearsPossessionGrounds.rentArrears, rentArrearsPossessionGrounds.seriousRentArrears, rentArrearsPossessionGrounds.persistentDelayInPayingRent],
        otherGrounds: rentArrearsPossessionGrounds.yes
      });
      await performAction('selectYourPossessionGrounds', {
        mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet, whatAreYourGroundsForPossession.mandatory.ownerOccupier],
        discretionary: [whatAreYourGroundsForPossession.discretionary.domesticViolence14A, whatAreYourGroundsForPossession.discretionary.rentArrears],
      });
      await performAction('enterReasonForPossession',
      [whatAreYourGroundsForPossession.mandatory.holidayLet,whatAreYourGroundsForPossession.mandatory.ownerOccupier,
        whatAreYourGroundsForPossession.discretionary.domesticViolence14A]);
      await performValidation('mainHeader', preActionProtocol.mainHeader);
      await performAction('selectPreActionProtocol', preActionProtocol.yes);
      await performValidation('mainHeader', mediationAndSettlement.mainHeader);
      await performAction('selectMediationAndSettlement', {
        attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
        settlementWithDefendantsOption: mediationAndSettlement.no,
      });
      await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
      await performValidation('text', { "text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink" })
      await performValidation('text', { "text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText" });
      await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.yes);
      await performValidation('mainHeader', noticeDetails.mainHeader);
      await performAction('selectNoticeDetails', {
        howDidYouServeNotice: noticeDetails.byFirstClassPost,
        day: '16', month: '07', year: '1985', files: 'NoticeDetails.pdf'
      });
      await performValidation('mainHeader', rentDetails.mainHeader);
      await performAction('provideRentDetails', { rentFrequencyOption: 'weekly', rentAmount: '800' });
      await performValidation('mainHeader', dailyRentAmount.mainHeader);
      await performAction('selectDailyRentAmount', {
        calculateRentAmount: '£114.29',
        unpaidRentInteractiveOption: dailyRentAmount.no,
        unpaidRentAmountPerDay: '20'
      });
      await performValidation('mainHeader', moneyJudgment.mainHeader);
      await performAction('selectMoneyJudgment', moneyJudgment.yes);
      await performValidation('mainHeader', claimantCircumstances.mainHeader);
      await performAction('selectClaimantCircumstances', {
        circumstanceOption: claimantCircumstances.yes,
        claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
      });
      await performValidation('mainHeader', defendantCircumstances.mainHeader);
      await performAction('selectDefendantCircumstances', defendantCircumstances.yes);
      await performValidation('mainHeader', alternativesToPossession.mainHeader);
      await performAction('selectAlternativesToPossession');
      await performValidation('mainHeader', claimingCosts.mainHeader);
      await performAction('selectClaimingCosts', claimingCosts.yes);
      await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
      await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yes);
      await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
      await performAction('clickButton', underlesseeOrMortgageeEntitledToClaim.continue);
      await performAction('wantToUploadDocuments', {
        question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
        option: wantToUploadDocuments.no
      });
      await performAction('selectApplications', applications.yes);
      await performAction('selectLanguageUsed', { question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english });
      await performAction('completingYourClaim', completeYourClaim.submitAndClaimNow);
      await performAction('clickButton', statementOfTruth.continue);
      await performAction('clickButton', checkYourAnswers.saveAndContinue);
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    }
  }
}
