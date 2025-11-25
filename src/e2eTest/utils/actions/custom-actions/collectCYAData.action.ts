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
  async execute(_page: Page, action: string, data?: actionRecord, _data?: actionData): Promise<void> {
    if (!data?.question || data.answer == null) return;

    const dataStore = action === 'collectCYAAddressData' ? cyaAddressData : cyaData;
    const question = typeof data.question === 'string' ? data.question.trim() : String(data.question);
    const answer = Array.isArray(data.answer) ? data.answer.join(', ') : String(data.answer);
    const actionName = data.actionName as string;

    if (!dataStore.collectedQAPairs) { dataStore.collectedQAPairs = []; }

    dataStore.collectedQAPairs.push({ step: actionName, question, answer });
  }
}
