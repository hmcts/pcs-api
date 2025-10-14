import {IAction} from '@utils/interfaces/action.interface';
import {CreateCaseAction} from '@utils/actions/custom-actions/createCase.action';
import {ClickTabAction} from '@utils/actions/element-actions/clickTab.action';
import {InputTextAction} from '@utils/actions/element-actions/inputText.action';
import {CheckAction} from '@utils/actions/element-actions/check.action';
import {SelectAction} from '@utils/actions/element-actions/select.action';
import {LoginAction} from "@utils/actions/custom-actions/login.action";
import {NavigateToUrl} from "@utils/actions/custom-actions/navigateToUrl.action";
import {ClickButtonAction} from "@utils/actions/element-actions/clickButton.action";
import {ClickRadioButton} from "@utils/actions/element-actions/clickRadioButton.action";
import {EnforcementAction} from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';


export class ActionRegistry {
  private static actions: Map<string, IAction> = new Map([
    ['clickButton', new ClickButtonAction()],
    ['clickButtonAndVerifyPageNavigation', new ClickButtonAction()],
    ['verifyPageAndClickButton', new ClickButtonAction()],
    ['clickTab', new ClickTabAction()],
    ['inputText', new InputTextAction()],
    ['check', new CheckAction()],
    ['select', new SelectAction()],
    ['loginEnforcement', new EnforcementAction()],
    ['navigateToUrl', new NavigateToUrl()],
    ['clickRadioButton', new ClickRadioButton()],
    ['caseFilter', new EnforcementAction()],
    ['selectAddress', new CreateCaseAction()],
    ['extractCaseIdFromAlert', new CreateCaseAction()],
    ['selectJurisdictionCaseTypeEvent', new CreateCaseAction()],
    ['housingPossessionClaim', new CreateCaseAction()],
    ['selectAddress', new CreateCaseAction()]
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
