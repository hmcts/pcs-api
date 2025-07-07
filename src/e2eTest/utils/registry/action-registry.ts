// action-registry.ts
import { IAction } from '../interfaces/action.interface';
import { ClickAction } from '../actions/element-actions/click.action';
import { FillAction } from '../actions/element-actions/fill.action';
import { ClearAction } from '../actions/element-actions/clear.action';
import { CheckAction } from '../actions/element-actions/check.action';
import { DoubleClickAction } from '../actions/element-actions/double-click.action';
import { SelectAction } from '../actions/element-actions/select.action';
import { LogOutAction } from '../actions/custom-actions/logOut.action';
import {LogInAction} from "../actions/custom-actions/logIn.action";

export class ActionRegistry {
  private static actions: Map<string, IAction> = new Map([
    ['click', new ClickAction()],
    ['fill', new FillAction()],
    ['clear', new ClearAction()],
    ['check', new CheckAction()],
    ['select', new SelectAction()],
    ['doubleClick', new DoubleClickAction()],
    ['logIn', new LogInAction()],
    ['logOut', new LogOutAction()],
  ]);

  static getAction(actionName: string): IAction {
    const action = this.actions.get(actionName);
    if (!action) {
      throw new Error(`Action '${actionName}' is not registered. Available actions: ${Array.from(this.actions.keys()).join(', ')}`);
    }
    return action;
  }

  static registerAction(actionName: string, action: IAction): void {
    this.actions.set(actionName, action);
  }

  static getAvailableActions(): string[] {
    return Array.from(this.actions.keys());
  }
}
