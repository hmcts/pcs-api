/**
 * CYA Field Collector
 * Collect CYA data directly in actions by passing action name, question and answer.
 * Consolidated file containing all CYA data structures, types, and collection functions.
 */

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

function normalizeAnswer(answer: any): string {
  return Array.isArray(answer) ? answer.join(', ') : String(answer);
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
  collectData(cyaAddressData, actionName, question.trim(), normalizeAnswer(answer).trim());
}

export function collectCYAData(actionName: string, question: string | number | boolean | object | string[] | object[], answer: any): void {
  if (!question || answer === undefined || answer === null) return;
  const questionStr = typeof question === 'string' ? question.trim() : String(question);
  collectData(cyaData, actionName, questionStr, normalizeAnswer(answer).trim());
}

