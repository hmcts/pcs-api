import { Page } from '@playwright/test';
import { initializeExecutor } from '@utils/controller';
import { performAction, performValidation } from '@utils/controller';
import { actionData, actionRecord, IAction } from '@utils/interfaces/action.interface';
import {
  home, addressDetails, additionalReasonsForPossession, alternativesToPossession, applications, checkYourAnswers, claimantCircumstances, claimantName,
  claimantType, claimingCosts, claimType, completeYourClaim, contactPreferences, defendantCircumstances, defendantDetails,
  provideMoreDetailsOfClaim, groundsForPossession, languageUsed, mediationAndSettlement, noticeOfYourIntention, preActionProtocol, statementOfTruth, tenancyLicenceDetails, underlesseeOrMortgageeEntitledToClaim,
  wantToUploadDocuments
} from '@data/page-data';

export class MakeClaimAction implements IAction {
  async execute(
    page: Page,
    action: string,
    fieldName?: actionData | actionRecord,
    value?: actionData | actionRecord
  ): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createNewCase', () => this.createNewCase(page, fieldName as actionData)],
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
        addressIndex: addressDetails.addressIndex,
      });
      await performValidation('bannerAlert', 'Case #.* has been created.');
      await performAction('extractCaseIdFromAlert');
      await performAction(
        'clickButtonAndVerifyPageNavigation',
        provideMoreDetailsOfClaim.continue,
        claimantType.mainHeader
      );
      await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
      await performAction('selectClaimType', claimType.no);
      await performAction('selectClaimantName', claimantName.yes);
      await performAction('clickButtonAndVerifyPageNavigation', claimantName.continue, contactPreferences.mainHeader);
      await performAction('selectContactPreferences', {
        notifications: contactPreferences.yes,
        correspondenceAddress: contactPreferences.yes,
        phoneNumber: contactPreferences.no,
      });
      await performAction('defendantDetails', {
        name: defendantDetails.no,
        correspondenceAddress: defendantDetails.no,
        email: defendantDetails.no,
      });
      await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
      await performAction('selectTenancyOrLicenceDetails', {
        tenancyOrLicenceType: tenancyLicenceDetails.introductoryTenancy,
      });
      await performValidation('mainHeader', groundsForPossession.mainHeader);
      await performAction('selectGroundsForPossession', { groundsRadioInput: groundsForPossession.no });
      await performAction('enterReasonForPossession', [groundsForPossession.noGrounds]);
      await performValidation('mainHeader', preActionProtocol.mainHeader);
      await performAction('selectPreActionProtocol', preActionProtocol.no);
      await performValidation('mainHeader', mediationAndSettlement.mainHeader);
      await performAction('selectMediationAndSettlement', {
        attemptedMediationWithDefendantsOption: mediationAndSettlement.no,
        settlementWithDefendantsOption: mediationAndSettlement.no,
      });
      await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
      await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.no);
      await performValidation('mainHeader', claimantCircumstances.mainHeader);
      await performAction('selectClaimantCircumstances', {
        circumstanceOption: claimantCircumstances.no
      });
      await performValidation('mainHeader', defendantCircumstances.mainHeader);
      await performAction('selectDefendantCircumstances', defendantCircumstances.no);
      await performValidation('mainHeader', alternativesToPossession.mainHeader);
      await performAction('selectAlternativesToPossession');
      await performValidation('mainHeader', claimingCosts.mainHeader);
      await performAction('selectClaimingCosts', claimingCosts.no);
      await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
      await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.no);
      await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
      await performAction('selectUnderlesseeOrMortgageeEntitledToClaim', {
        question: underlesseeOrMortgageeEntitledToClaim.entitledToClaimRelief,
        option: underlesseeOrMortgageeEntitledToClaim.no
      });
      await performAction('wantToUploadDocuments', {
        question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
        option: wantToUploadDocuments.no,
      });
      await performAction('selectApplications', applications.no);
      await performAction('selectLanguageUsed', {
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.english,
      });
      await performAction('completingYourClaim', completeYourClaim.submitAndClaimNow);
      await performAction('clickButton', statementOfTruth.continue);
      await performAction('clickButton', checkYourAnswers.saveAndContinue);
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    }
  }
}
