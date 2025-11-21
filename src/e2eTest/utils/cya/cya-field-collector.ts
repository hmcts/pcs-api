/**
 * CYA Field Collector
 * Collect CYA data directly in actions by passing action name, question and answer.
 */

import { cyaAddressData, CYAAddressData } from '@utils/data/cya-address-data';
import { cyaData, CYAData } from '@utils/data/cya-data';

function normalizeAnswer(answer: any): string {
  if (Array.isArray(answer)) {
    return answer.join(', ');
  }
  return String(answer);
}

function collectData(
  dataStore: CYAData | CYAAddressData,
  actionName: string,
  question: string,
  answer: string,
  logPrefix: string
): void {
  if (!dataStore.collectedQAPairs) {
    dataStore.collectedQAPairs = [];
  }

  // Prevent duplicate: same question AND answer AND step
  // Allows same Q&A from different pages/actions (step indicates which page)
  const isDuplicate = dataStore.collectedQAPairs.some(
    pair => pair.question === question && pair.answer === answer && pair.step === actionName
  );

  if (isDuplicate) {
    return;
  }

  dataStore.collectedQAPairs.push({ step: actionName, question, answer });
  console.log(`${logPrefix} Collected Q&A Pair:`, { step: actionName, question, answer });
}

export async function collectCYAAddressData(actionName: string, question: string, answer: any): Promise<void> {
  if (!question || answer === undefined || answer === null) return;

  const questionTrimmed = question.trim();
  const answerTrimmed = normalizeAnswer(answer).trim();
  collectData(cyaAddressData, actionName, questionTrimmed, answerTrimmed, '🏠 [Address CYA]');
}

export async function collectCYAData(actionName: string, question: string | number | boolean | object | string[] | object[], answer: any): Promise<void> {
  if (!question || answer === undefined || answer === null) return;

  const questionTrimmed = typeof question === 'string' ? question.trim() : String(question);
  const answerTrimmed = normalizeAnswer(answer).trim();
  collectData(cyaData, actionName, questionTrimmed, answerTrimmed, '📝 [Final CYA]');
}

