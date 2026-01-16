import { IAction } from '@utils/interfaces/action.interface';
import { ClickTabAction } from '@utils/actions/element-actions/clickTab.action';
import { InputTextAction } from '@utils/actions/element-actions/inputText.action';
import { CheckAction } from '@utils/actions/element-actions/check.action';
import { SelectAction } from '@utils/actions/element-actions/select.action';
import { NavigateToUrlAction } from '@utils/actions/custom-actions/navigateToUrl.action';
import { ClickButtonAction } from '@utils/actions/element-actions/clickButton.action';
import { ClickRadioButtonAction } from '@utils/actions/element-actions/clickRadioButton.action';
import { LoginAction } from '@utils/actions/custom-actions/login.action';
import { SearchCaseAction } from '@utils/actions/custom-actions/searchCase.action';
import { EnforcementAction } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { handleCookieConsentAction } from '@utils/actions/custom-actions/handleCookieConsent.action';
import { CreateCaseAPIAction } from '@utils/actions/custom-actions/createCaseAPI.action';
import { ExpandSummaryAction } from '@utils/actions/element-actions';
import { ClickLinkAndVerifyNewTabTitleAction } from '@utils/actions/element-actions/clickLinkAndVerifyNewTabTitle.action';

export class ActionEnforcementRegistry {
  private static actions: Map<string, IAction> = new Map<string, IAction>([
    ['clickButton', new ClickButtonAction()],
    ['clickButtonAndVerifyPageNavigation', new ClickButtonAction()],
    ['verifyPageAndClickButton', new ClickButtonAction()],
    ['clickTab', new ClickTabAction()],
    ['inputText', new InputTextAction()],
    ['check', new CheckAction()],
    ['select', new SelectAction()],
    ['login', new LoginAction()],
    ['navigateToUrl', new NavigateToUrlAction()],
    ['handleCookieConsent', new handleCookieConsentAction()],
    ['clickRadioButton', new ClickRadioButtonAction()],
    ['clickLinkAndVerifyNewTabTitle', new ClickLinkAndVerifyNewTabTitleAction()],
    ['expandSummary', new ExpandSummaryAction()],
    ['filterCaseFromCaseList', new SearchCaseAction()],
    ['validateWritOrWarrantFeeAmount', new EnforcementAction()],
    ['validateGetQuoteFromBailiffLink', new EnforcementAction()],
    ['selectApplicationType', new EnforcementAction()],
    ['selectNameAndAddressForEviction', new EnforcementAction()],
    ['confirmDefendantsDOB', new EnforcementAction()],
    ['enterDefendantsDOB', new EnforcementAction()],
    ['getDefendantDetails', new EnforcementAction()],
    ['selectPeopleWhoWillBeEvicted', new EnforcementAction()],
    ['selectPeopleYouWantToEvict', new EnforcementAction()],
    ['selectPermissionFromJudge', new EnforcementAction()],
    ['selectEveryoneLivingAtTheProperty', new EnforcementAction()],
    ['selectRiskPosedByEveryoneAtProperty', new EnforcementAction()],
    ['provideDetailsViolentOrAggressiveBehaviour', new EnforcementAction()],
    ['provideDetailsFireArmPossession', new EnforcementAction()],
    ['provideDetailsCriminalOrAntisocialBehavior', new EnforcementAction()],
    ['provideDetailsVerbalOrWrittenThreats', new EnforcementAction()],
    ['provideDetailsGroupProtestsEviction', new EnforcementAction()],
    ['provideDetailsPoliceOrSocialServiceVisits', new EnforcementAction()],
    ['provideDetailsAnimalsAtTheProperty', new EnforcementAction()],
    ['selectVulnerablePeopleInTheProperty', new EnforcementAction()],
    ['provideDetailsAnythingElseHelpWithEviction', new EnforcementAction()],
    ['accessToProperty', new EnforcementAction()],
    ['createCaseAPI', new CreateCaseAPIAction()],
    ['submitCaseAPI', new CreateCaseAPIAction()],
    ['provideMoneyOwed', new EnforcementAction()],
    ['provideLegalCosts', new EnforcementAction()],
    ['provideLandRegistryFees', new EnforcementAction()],
    ['provideAmountToRePay', new EnforcementAction()],
    ['validateAmountToRePayTable', new EnforcementAction()],
    ['selectLanguageUsed', new EnforcementAction()],
    ['confirmSuspendedOrder', new EnforcementAction()],
    ['selectStatementOfTruthOne', new EnforcementAction()],
    ['selectStatementOfTruthTwo', new EnforcementAction()],
    ['inputErrorValidation', new EnforcementAction()],
    ['generateRandomString', new EnforcementAction()],
  ]);

  static getAction(actionName: string): IAction {
    const action = this.actions.get(actionName);
    if (!action) {
      throw new Error(
        `Action '${actionName}' is not registered. Available actions: ${Array.from(this.actions.keys()).join(', ')}`
      );
    }
    return action;
  }

  static getAvailableActions(): string[] {
    return Array.from(this.actions.keys());
  }
}