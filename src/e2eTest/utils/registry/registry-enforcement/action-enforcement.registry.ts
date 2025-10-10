import {IAction} from '../../interfaces/action.interface';
import {ClickTabAction} from '../../actions/element-actions/clickTab.action';
import {InputTextAction} from '../../actions/element-actions/inputText.action';
import {CheckAction} from '../../actions/element-actions/check.action';
import {SelectAction} from '../../actions/element-actions/select.action';
import {LoginAction} from "../../actions/custom-actions/login.action";
import {NavigateToUrl} from "@utils/actions/custom-actions/navigateToUrl.action";
import {ClickButtonAction} from "@utils/actions/element-actions/clickButton.action";
import {ClickRadioButton} from "@utils/actions/element-actions/clickRadioButton.action";

export class ActionRegistry {
  private static actions: Map<string, IAction> = new Map([
    ['clickButton', new ClickButtonAction()],
    ['clickButtonAndVerifyPageNavigation', new ClickButtonAction()],
    ['verifyPageAndClickButton', new ClickButtonAction()],
    ['clickTab', new ClickTabAction()],
    ['inputText', new InputTextAction()],
    ['check', new CheckAction()],
    ['select', new SelectAction()],
    ['createUserAndLogin', new LoginAction()],
    ['login', new LoginAction()],
    ['navigateToUrl', new NavigateToUrl()],
    ['clickRadioButton', new ClickRadioButton()]
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
