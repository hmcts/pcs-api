import { Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces';

export interface CollectedQAPair {
  step?: string;
  question?: string;
  answer?: string;
}

export interface CYAData {
  collectedQAPairs?: CollectedQAPair[];
}

export interface CYAAddressData {
  collectedQAPairs?: CollectedQAPair[];
}

export let cyaData: CYAData = {};
export let cyaAddressData: CYAAddressData = {};

export function resetCYAData(): void {
  cyaData = {};
}

export function resetCYAAddressData(): void {
  cyaAddressData = {};
}

function collectData(
  dataStore: CYAData | CYAAddressData,
  actionName: string,
  question: string,
  answer: string
): void {
  if (!dataStore.collectedQAPairs) {
    dataStore.collectedQAPairs = [];
  }

  if (!dataStore.collectedQAPairs.some(
    pair => pair.question === question && pair.answer === answer && pair.step === actionName
  )) {
    dataStore.collectedQAPairs.push({ step: actionName, question, answer });
  }
}

export function collectCYAAddressData(actionName: string, question: string, answer: any): void {
  if (!question || answer === undefined || answer === null) return;
  const normalizedAnswer = Array.isArray(answer) ? answer.join(', ') : String(answer);
  collectData(cyaAddressData, actionName, question.trim(), normalizedAnswer.trim());
}

export function collectCYAData(actionName: string, question: string | number | boolean | object | string[] | object[], answer: any): void {
  if (!question || answer === undefined || answer === null) return;
  const questionStr = typeof question === 'string' ? question.trim() : String(question);
  const normalizedAnswer = Array.isArray(answer) ? answer.join(', ') : String(answer);
  collectData(cyaData, actionName, questionStr, normalizedAnswer.trim());
}

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
    if (!data?.question || data.answer == null) return;
    collectCYAData((data.actionName as string) || 'collectCYAData', data.question, data.answer);
  }

  private async collectCYAAddressData(data: actionRecord): Promise<void> {
    if (!data?.question || data.answer == null) return;
    collectCYAAddressData((data.actionName as string) || 'collectCYAAddressData', String(data.question), String(data.answer));
  }
}
