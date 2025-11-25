/**
 * CYA Field Collector Action
 * Collect CYA data directly in actions by passing action name, question and answer.
 * Consolidated file containing all CYA data structures, types, collection functions, and action implementation.
 */

import { Page } from '@playwright/test';
import { IAction, actionData, actionRecord } from '@utils/interfaces';

/**
 * Shared types for CYA data structures
 */
export interface CollectedQAPair {
  step?: string;
  question?: string;
  answer?: string;
}

/**
 * Data structure to store collected Q&A pairs for Final CYA validation
 */
export interface CYAData {
  collectedQAPairs?: CollectedQAPair[];
}

/**
 * Data structure to store collected Q&A pairs for Address CYA validation
 */
export interface CYAAddressData {
  collectedQAPairs?: CollectedQAPair[];
}

// Global instances to store CYA data during test execution
export let cyaData: CYAData = {};
export let cyaAddressData: CYAAddressData = {};

// Function to reset Final CYA data (useful for test cleanup)
export function resetCYAData(): void {
  cyaData = {};
}

// Function to reset Address CYA data (useful for test cleanup)
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

  // Prevent duplicate: same question AND answer AND step
  const isDuplicate = dataStore.collectedQAPairs.some(
    pair => pair.question === question && pair.answer === answer && pair.step === actionName
  );

  if (!isDuplicate) {
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
