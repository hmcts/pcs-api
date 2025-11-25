import { Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces';
import { collectCYAData, collectCYAAddressData } from '@utils/cya/cya-field-collector';

export class CollectCYADataAction implements IAction {
  async execute(_page: Page, action: string, fieldName?: actionData | actionRecord, _data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['collectCYAData', () => this.collectCYAData(fieldName as actionRecord)],
      ['collectCYAAddressData', () => this.collectCYAAddressData(fieldName as actionRecord)],
    ]);

    const actionHandler = actionsMap.get(action);
    if (actionHandler) {
      await actionHandler();
    } else {
      throw new Error(`Unknown action: ${action}`);
    }
  }

  private async collectCYAData(data: actionRecord): Promise<void> {
    if (!data || !data.question || data.answer === undefined || data.answer === null) {
      return;
    }
    const actionName = (data.actionName as string) || 'collectCYAData';
    collectCYAData(actionName, data.question, data.answer);
  }

  private async collectCYAAddressData(data: actionRecord): Promise<void> {
    if (!data || !data.question || data.answer === undefined || data.answer === null) {
      return;
    }
    const actionName = (data.actionName as string) || 'collectCYAAddressData';
    collectCYAAddressData(actionName, String(data.question), String(data.answer));
  }
}
