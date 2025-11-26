import { Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces';

export interface CollectedQAPair {
  step?: string;
  question?: string;
  answer?: string;
}

export interface CYADataStore {
  collectedQAPairs?: CollectedQAPair[];
}

export type CYAData = CYADataStore;
export type CYAAddressData = CYADataStore;

export let cyaData: CYAData = {};
export let cyaAddressData: CYAAddressData = {};

export function resetCYAData(): void {
  cyaData = {};
}

export function resetCYAAddressData(): void {
  cyaAddressData = {};
}

export class CollectCYADataAction implements IAction {
  async execute(_page: Page, action: string, fieldName?: actionData | actionRecord, _data?: actionData): Promise<void> {
    const data = fieldName as actionRecord;
    if (!data?.question || data.answer == null) return;

    const dataStore = action === 'collectCYAAddressData' ? cyaAddressData : cyaData;
    const questionStr = String(data.question);
    const question = questionStr === ' ' ? ' ' : questionStr.trim();
    const answer = (Array.isArray(data.answer) ? data.answer.join(', ') : String(data.answer)).trim();
    const actionName = (data.actionName as string) || action;

    if (!dataStore.collectedQAPairs) {
      dataStore.collectedQAPairs = [];
    }

    const isDuplicate = dataStore.collectedQAPairs.some(
      pair => pair.question === question && pair.answer === answer && pair.step === actionName
    );
    if (isDuplicate) {
      throw new Error(
        `Duplicate Q&A pair detected in action "${actionName}": Question="${question}", Answer="${answer}". ` +
        `- the same question should not be collected twice under the same action name.`
      );
    }
    dataStore.collectedQAPairs.push({ step: actionName, question, answer });
  }
}
