import { Page } from "@playwright/test";
import {initializeExecutor, performValidations} from "@utils/controller";
import { performAction, performValidation } from "@utils/controller";
import { actionData, actionRecord, IAction } from "@utils/interfaces/action.interface";
import {
  home,
  addressDetails,
  additionalReasonsForPossession,
  alternativesToPossession,
  applications,
  checkYourAnswers,
  claimantCircumstances,
  claimantName,
  claimantType,
  claimingCosts,
  claimType,
  completeYourClaim,
  contactPreferences,
  dailyRentAmount,
  defendantCircumstances,
  defendantDetails,
  groundsForPossession,
  languageUsed,
  mediationAndSettlement,
  moneyJudgment,
  noticeDetails,
  noticeOfYourIntention,
  preActionProtocol,
  provideMoreDetailsOfClaim,
  rentArrearsPossessionGrounds,
  rentDetails,
  statementOfTruth,
  tenancyLicenceDetails,
  underlesseeOrMortgageeEntitledToClaim,
  wantToUploadDocuments,
  whatAreYourGroundsForPossession,
  rentArrearsOrBreachOfTenancy, reasonsForPossession, housingAct, reasonsForRequestingASuspensionOrder, propertyDetails
} from "@data/page-data";

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
      await performAction('provideMoreDetailsOfClaim');
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
        name: defendantDetails.no,
        correspondenceAddress: defendantDetails.no,
        email: defendantDetails.no,
      });
      await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
      await performAction('selectTenancyOrLicenceDetails', {
        tenancyOrLicenceType: tenancyLicenceDetails.flexibleTenancy});
      await performAction('selectYourPossessionGrounds', {
        discretionary: [whatAreYourGroundsForPossession.discretionary.rentArrearsOrBreachOfTenancy]
      });
      await performValidation('mainHeader', rentArrearsOrBreachOfTenancy.mainHeader);
      await performAction('selectRentArrearsOrBreachOfTenancy', {
        rentArrearsOrBreach: [rentArrearsOrBreachOfTenancy.breachOfTenancy]
      });
      await performAction('enterReasonForPossession', [reasonsForPossession.breachOfTenancy]);
      await performValidation('mainHeader', preActionProtocol.mainHeader);
      await performAction('selectPreActionProtocol', preActionProtocol.yes);
      await performAction('selectMediationAndSettlement', {
        attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
        settlementWithDefendantsOption: mediationAndSettlement.no,
      });
      await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
      await performAction('selectNoticeOfYourIntention', {
        question: noticeOfYourIntention.servedNoticeInteractiveText,
        option: noticeOfYourIntention.yes,
      });
      await performAction('selectNoticeDetails', {
        howDidYouServeNotice: noticeDetails.byEmail,
        explanationLabel: noticeDetails.explainHowServedByEmailLabel,
        explanation: noticeDetails.byEmailExplanationInput});
      await performValidation('mainHeader', claimantCircumstances.mainHeader);
      await performAction('selectClaimantCircumstances', {
        circumstanceOption: claimantCircumstances.no,
        claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
      });
      await performValidation('mainHeader', defendantCircumstances.mainHeader);
      await performAction('selectDefendantCircumstances', defendantCircumstances.no);
      await performValidation('mainHeader', alternativesToPossession.mainHeader);
      await performAction('selectAlternativesToPossession', {question: alternativesToPossession.suspensionOrDemotion
        , option: [alternativesToPossession.suspensionOfRightToBuy]});
      await performValidation('mainHeader', housingAct.mainHeader);
      await performAction('selectHousingAct', [{question: housingAct.suspensionOfRightToBuy.whichSection
        , option: housingAct.suspensionOfRightToBuy.section121AHousingAct1985}]);
      await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
      await performValidation('mainHeader', reasonsForRequestingASuspensionOrder.mainHeader);
      await performAction('enterReasonForSuspensionOrder', reasonsForRequestingASuspensionOrder.requestSuspensionOrderQuestion);
      await performValidation('mainHeader', claimingCosts.mainHeader);
      await performAction('selectClaimingCosts', claimingCosts.no);
      await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
      await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yes);
      await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
      await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
        question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
        option: underlesseeOrMortgageeEntitledToClaim.no});
      await performAction('wantToUploadDocuments', {
        question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
        option: wantToUploadDocuments.no
      });
      await performAction('selectApplications', applications.yes);
      await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
      await performAction('completingYourClaim', completeYourClaim.saveItForLater);
      await performAction('clickButton', checkYourAnswers.saveAndContinue);
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
      await performValidations('address information entered',
        ['formLabelValue', propertyDetails.buildingAndStreetLabel, addressDetails.buildingAndStreet],
        ['formLabelValue', propertyDetails.townOrCityLabel, addressDetails.townOrCity],
        ['formLabelValue', propertyDetails.postcodeZipcodeLabel, addressDetails.walesCourtAssignedPostcode],
        ['formLabelValue', propertyDetails.countryLabel, addressDetails.country]);
       }
  }
}

