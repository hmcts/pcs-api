import { IAction } from '../interfaces/action.interface';
import { ClickTabAction } from '../actions/element-actions/clickTab.action';
import { InputTextAction } from '../actions/element-actions/inputText.action';
import { ClearAction } from '../actions/element-actions/clear.action';
import { CheckAction } from '../actions/element-actions/check.action';
import { DoubleClickAction } from '../actions/element-actions/double-click.action';
import { SelectAction } from '../actions/element-actions/select.action';
import { LoginAction } from "../actions/custom-actions/login.action";
import { navigateToUrl } from "@utils/actions/custom-actions/navigateToUrl.action";
import { CreateCaseAction } from "@utils/actions/custom-actions/createCase.action";
import { ClickButtonAction } from "../actions/element-actions/clickButton.action";
import { ClickRadioButton } from "../actions/element-actions/clickRadioButton.action";
export class ActionRegistry {
  private static actions: Map<string, IAction> = new Map([
    ['clickButton', new ClickButtonAction()],
    ['clickTab', new ClickTabAction()],
    ['inputText', new InputTextAction()],
    ['clear', new ClearAction()],
    ['check', new CheckAction()],
    ['select', new SelectAction()],
    ['doubleClick', new DoubleClickAction()],
    ['login', new LoginAction()],
    ['navigateToUrl', new navigateToUrl()],
    ['createCase', new CreateCaseAction()],
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
