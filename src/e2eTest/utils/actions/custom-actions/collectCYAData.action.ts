import { Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces';

export interface CollectedQAPair { step?: string;question: string;answer: string;}

export interface CYADataStore { collectedQAPairs: CollectedQAPair[]; }

export let cyaData: CYADataStore = { collectedQAPairs: [] };
export let cyaAddressData: CYADataStore = { collectedQAPairs: [] };

export function resetCYAData(): void {
  cyaData = { collectedQAPairs: [] };
  cyaAddressData = { collectedQAPairs: [] };
}

export class CollectCYADataAction implements IAction {
  async execute(_page: Page, action: string, fieldName?: actionData | actionRecord, _value?: actionData | actionRecord): Promise<void> {
    const data = fieldName as actionRecord;
    if (!data?.question || data.answer == null) return;

    const dataStore = action === 'collectCYAAddressData' ? cyaAddressData : cyaData;

    const question = String(data.question) === ' ' ? ' ' : String(data.question).trim();
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
        `The same question should not be collected twice under the same action name.`
      );
    }
    dataStore.collectedQAPairs.push({ step: actionName, question, answer });
  }
}
