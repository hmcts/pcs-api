import {IAction} from '@utils/interfaces';
import {ClickTabAction} from '@utils/actions/element-actions/clickTab.action';
import {InputTextAction} from '@utils/actions/element-actions/inputText.action';
import {CheckAction} from '@utils/actions/element-actions/check.action';
import {SelectAction} from '@utils/actions/element-actions/select.action';
import {LoginAction} from '@utils/actions/custom-actions/login.action';
import {NavigateToUrlAction} from '@utils/actions/custom-actions/navigateToUrl.action';
import {CreateCaseAction} from '@utils/actions/custom-actions/createCase.action';
import {ClickButtonAction} from '@utils/actions/element-actions/clickButton.action';
import {ClickRadioButtonAction} from '@utils//actions/element-actions/clickRadioButton.action';
import {UploadFileAction} from '@utils/actions/element-actions/uploadFile.action';
import {CreateCaseWalesAction} from '@utils/actions/custom-actions/createCaseWales.action';
import {SearchCaseAction} from '@utils/actions/custom-actions/searchCase.action';
import {signOutAction} from '@utils/actions/custom-actions/signOut.action';
import {ClickLinkAndVerifyNewTabTitleAction} from '@utils/actions/element-actions/clickLinkAndVerifyNewTabTitle.action';
import {CreateCaseAPIAction} from '@utils/actions/custom-actions/createCaseAPI.action';
import {ExpandSummaryAction} from '@utils/actions/element-actions';

export class ActionRegistry {
  private static actions: Map<string, IAction> = new Map<string, IAction>([
    ['clickButton', new ClickButtonAction()],
    ['clickButtonAndVerifyPageNavigation', new ClickButtonAction()],
    ['verifyPageAndClickButton', new ClickButtonAction()],
    ['clickTab', new ClickTabAction()],
    ['clickRadioButton', new ClickRadioButtonAction()],
    ['inputText', new InputTextAction()],
    ['check', new CheckAction()],
    ['select', new SelectAction()],
    ['expandSummary', new ExpandSummaryAction()],
    ['createUserAndLogin', new LoginAction()],
    ['login', new LoginAction()],
    ['navigateToUrl', new NavigateToUrlAction()],
    ['signOut', new signOutAction()],
    ['uploadFile', new UploadFileAction()],
    ['selectAddress', new CreateCaseAction()],
    ['submitAddressCheckYourAnswers', new CreateCaseAction()],
    ['extractCaseIdFromAlert', new CreateCaseAction()],
    ['selectResumeClaimOption', new CreateCaseAction()],
    ['selectClaimantType', new CreateCaseAction()],
    ['addDefendantDetails', new CreateCaseAction()],
    ['selectRentArrearsPossessionGround', new CreateCaseAction()],
    ['selectJurisdictionCaseTypeEvent', new CreateCaseAction()],
    ['enterTestAddressManually', new CreateCaseAction()],
    ['createCaseAPI', new CreateCaseAPIAction()],
    ['submitCaseAPI', new CreateCaseAPIAction()],
    ['deleteCaseRole', new CreateCaseAPIAction()],
    ['selectClaimType', new CreateCaseAction()],
    ['selectClaimantName', new CreateCaseAction()],
    ['selectClaimantDetails', new CreateCaseWalesAction()],
    ['selectContactPreferences', new CreateCaseAction()],
    ['housingPossessionClaim', new CreateCaseAction()],
    ['selectGroundsForPossession', new CreateCaseAction()],
    ['selectPreActionProtocol', new CreateCaseAction()],
    ['selectMediationAndSettlement', new CreateCaseAction()],
    ['selectNoticeOfYourIntention', new CreateCaseAction()],
    ['selectNoticeDetails', new CreateCaseAction()],
    ['selectBorderPostcode', new CreateCaseAction()],
    ['selectYourPossessionGrounds', new CreateCaseAction()],
    ['selectOtherGrounds', new CreateCaseAction()],
    ['selectTenancyOrLicenceDetails', new CreateCaseAction()],
    ['enterReasonForPossession', new CreateCaseAction()],
    ['reloginAndFindTheCase', new CreateCaseAction()],
    ['selectRentArrearsOrBreachOfTenancy', new CreateCaseAction()],
    ['provideRentDetails', new CreateCaseAction()],
    ['selectDailyRentAmount', new CreateCaseAction()],
    ['selectClaimantCircumstances', new CreateCaseAction()],
    ['provideDetailsOfRentArrears', new CreateCaseAction()],
    ['selectMoneyJudgment', new CreateCaseAction()],
    ['selectClaimingCosts', new CreateCaseAction()],
    ['selectLanguageUsed', new CreateCaseAction()],
    ['selectDefendantCircumstances', new CreateCaseAction()],
    ['selectApplications', new CreateCaseAction()],
    ['completingYourClaim', new CreateCaseAction()],
    ['selectAdditionalReasonsForPossession', new CreateCaseAction()],
    ['selectUnderlesseeOrMortgageeEntitledToClaim', new CreateCaseAction()],
    ['selectUnderlesseeOrMortgageeDetails', new CreateCaseAction()],
    ['enterReasonForDemotionOrder', new CreateCaseAction()],
    ['enterReasonForSuspensionAndDemotionOrder', new CreateCaseAction()],
    ['selectStatementOfExpressTerms', new CreateCaseAction()],
    ['selectAlternativesToPossession', new CreateCaseAction()],
    ['selectHousingAct', new CreateCaseAction()],
    ['enterReasonForSuspensionOrder', new CreateCaseAction()],
    ['searchCaseFromFindCase', new SearchCaseAction()],
    ['filterCaseFromCaseList', new SearchCaseAction()],
    ['selectClaimingCosts', new CreateCaseAction()],
    ['wantToUploadDocuments', new CreateCaseAction()],
    ['uploadAdditionalDocs', new CreateCaseAction()],
    ['clickButtonAndWaitForElement', new ClickButtonAction()],
    ['selectProhibitedConductStandardContract', new CreateCaseWalesAction()],
    ['selectOccupationContractOrLicenceDetails', new CreateCaseWalesAction()],
    ['provideMoreDetailsOfClaim', new CreateCaseAction()],
    ['clickLinkAndVerifyNewTabTitle', new ClickLinkAndVerifyNewTabTitleAction()],
    ['selectStatementOfTruth', new CreateCaseAction()],
    ['selectAsb', new CreateCaseWalesAction()],
    ['payClaimFee', new CreateCaseAction()],
    ['claimSaved', new CreateCaseAction()]
  ]);

  static getAction(actionName: string): IAction {
    const action = this.actions.get(actionName);
    if (!action) {
      throw new Error(`Action '${actionName}' is not registered. Available actions: ${Array.from(this.actions.keys()).join(', ')}`);
    }
    return action;
  }

  static getAvailableActions(): string[] {
    return Array.from(this.actions.keys());
  }
}
