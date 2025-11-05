import { Page } from "@playwright/test";
import { addressDetails } from "@data/page-data/addressDetails.page.data";
import { home } from "@data/page-data/home.page.data";
import { initializeExecutor } from "@utils/controller";
import { performAction, performValidation } from "@utils/controller";
import { actionData, actionRecord, IAction } from "@utils/interfaces/action.interface";
import { additionalReasonsForPossession } from "@data/page-data/additionalReasonsForPossession.page.data";
import { alternativesToPossession } from "@data/page-data/alternativesToPossession.page.data";
import { applications } from "@data/page-data/applications.page.data";
import { checkYourAnswers } from "@data/page-data/checkYourAnswers.page.data";
import { claimantCircumstances } from "@data/page-data/claimantCircumstances.page.data";
import { claimantName } from "@data/page-data/claimantName.page.data";
import { claimantType } from "@data/page-data/claimantType.page.data";
import { claimingCosts } from "@data/page-data/claimingCosts.page.data";
import { claimType } from "@data/page-data/claimType.page.data";
import { completeYourClaim } from "@data/page-data/completeYourClaim.page.data";
import { contactPreferences } from "@data/page-data/contactPreferences.page.data";
import { defendantCircumstances } from "@data/page-data/defendantCircumstances.page.data";
import { defendantDetails } from "@data/page-data/defendantDetails.page.data";
import { groundsForPossession } from "@data/page-data/groundsForPossession.page.data";
import { languageUsed } from "@data/page-data/languageUsed.page.data";
import { mediationAndSettlement } from "@data/page-data/mediationAndSettlement.page.data";
import { moneyJudgment } from "@data/page-data/moneyJudgment.page.data";
import { noticeOfYourIntention } from "@data/page-data/noticeOfYourIntention.page.data";
import { preActionProtocol } from "@data/page-data/preActionProtocol.page.data";
import { provideMoreDetailsOfClaim } from "@data/page-data/provideMoreDetailsOfClaim.page.data";
import { statementOfTruth } from "@data/page-data/statementOfTruth.page.data";
import { tenancyLicenceDetails } from "@data/page-data/tenancyLicenceDetails.page.data";
import { underlesseeOrMortgageeEntitledToClaim } from "@data/page-data/underlesseeOrMortgageeEntitledToClaim.page.data";
import { wantToUploadDocuments } from "@data/page-data/wantToUploadDocuments.page.data";

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
        name: defendantDetails.no,
        correspondenceAddress: defendantDetails.no,
        email: defendantDetails.no,
      });
      await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
      await performAction('selectTenancyOrLicenceDetails', {
        tenancyOrLicenceType: tenancyLicenceDetails.introductoryTenancy
      });
      await performValidation('mainHeader', groundsForPossession.mainHeader);
      await performAction('selectGroundsForPossession', { groundsRadioInput: groundsForPossession.no });
      await performAction('enterReasonForPossession', [groundsForPossession.noGrounds]);
      await performValidation('mainHeader', preActionProtocol.mainHeader);
      await performAction('selectPreActionProtocol', preActionProtocol.yes);
      await performValidation('mainHeader', mediationAndSettlement.mainHeader);
      await performAction('selectMediationAndSettlement', {
        attemptedMediationWithDefendantsOption: mediationAndSettlement.no,
        settlementWithDefendantsOption: mediationAndSettlement.no,
      });
      await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
      await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.no);
      await performValidation('mainHeader', moneyJudgment.mainHeader);
      await performAction('selectMoneyJudgment', moneyJudgment.no);
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
        option: wantToUploadDocuments.no
      });
      await performAction('selectApplications', applications.no);
      await performAction('selectLanguageUsed', { question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english });
      await performAction('completingYourClaim', completeYourClaim.submitAndClaimNow);
      await performAction('clickButton', statementOfTruth.continue);
      await performAction('clickButton', checkYourAnswers.saveAndContinue);
      await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    }
  }
}
