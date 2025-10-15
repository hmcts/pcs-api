import {IAction} from '@utils/interfaces/action.interface';
import {ClickTabAction} from '@utils/actions/element-actions/clickTab.action';
import {InputTextAction} from '@utils/actions/element-actions/inputText.action';
import {CheckAction} from '@utils/actions/element-actions/check.action';
import {SelectAction} from '@utils/actions/element-actions/select.action';
import {NavigateToUrl} from "@utils/actions/custom-actions/navigateToUrl.action";
import {ClickButtonAction} from "@utils/actions/element-actions/clickButton.action";
import {ClickRadioButton} from "@utils/actions/element-actions/clickRadioButton.action";
import {EnforcementAction} from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import {MakeClaimAction} from '@utils/actions/custom-actions/custom-actions-enforcement/makeClaim.action';

export class ActionEnforcementRegistry {
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
    ['makeClaim', new MakeClaimAction()],
    ['findCase', new EnforcementAction()],
    ['pickAnyCase', new EnforcementAction()],
    ['yourCases', new EnforcementAction()]
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
