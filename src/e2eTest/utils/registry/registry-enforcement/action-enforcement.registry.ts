import {IAction} from '@utils/interfaces/action.interface';
import {ClickTabAction} from '@utils/actions/element-actions/clickTab.action';
import {InputTextAction} from '@utils/actions/element-actions/inputText.action';
import {CheckAction} from '@utils/actions/element-actions/check.action';
import {SelectAction} from '@utils/actions/element-actions/select.action';
import {NavigateToUrlAction} from '@utils/actions/custom-actions/navigateToUrl.action';
import {ClickButtonAction} from '@utils/actions/element-actions/clickButton.action';
import {ClickRadioButtonAction} from '@utils/actions/element-actions/clickRadioButton.action';
import {MakeClaimAction} from '@utils/actions/custom-actions/custom-actions-enforcement/makeClaim.action';
import {LoginAction} from '@utils/actions/custom-actions/login.action';
import {SearchCaseAction} from '@utils/actions/custom-actions/searchCase.action';
import {EnforcementAction} from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';

export class ActionEnforcementRegistry {
  private static actions: Map<string, IAction> = new Map([
    ['clickButton', new ClickButtonAction()],
    ['clickButtonAndVerifyPageNavigation', new ClickButtonAction()],
    ['verifyPageAndClickButton', new ClickButtonAction()],
    ['clickTab', new ClickTabAction()],
    ['inputText', new InputTextAction()],
    ['check', new CheckAction()],
    ['select', new SelectAction()],
    ['login', new LoginAction()],
    ['navigateToUrl', new NavigateToUrlAction()],
    ['clickRadioButton', new ClickRadioButtonAction()],
    ['filterCaseFromCaseList', new SearchCaseAction()],
    ['createNewCase', new MakeClaimAction()],
    ['searchMyCaseFromFindCase', new SearchCaseAction()],
    ['selectFirstCaseFromTheFilter', new SearchCaseAction()],
    ['noCasesFoundAfterSearch', new SearchCaseAction()],
    ['selectApplicationType', new EnforcementAction()],
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
