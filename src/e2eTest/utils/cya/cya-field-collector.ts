/**
 * CYA Field Collector - Simple approach
 *
 * Collect CYA data directly in actions by passing action name, question and answer.
 * Question can come from spec, page.data, or be hardcoded.
 *
 * Usage:
 *   await collectCYAAddressData('selectAddress', 'Property address', addressString);
 *   await collectCYAData('selectClaimantType', 'Who is the claimant in this case?', caseData);
 */

import { cyaAddressData } from '@utils/data/cya-address-data';
import { cyaData } from '@utils/data/cya-data';

/**
 * Collect CYA data for Address CYA (first 2 pages: address and country)
 *
 * @param actionName - Name of the action collecting this data (e.g., 'selectAddress')
 * @param question - Question text as it appears on CYA (from spec, page.data, or hardcoded)
 * @param answer - Answer value (string, number, array, etc. - will be converted to string)
 */
export async function collectCYAAddressData(actionName: string, question: string, answer: any): Promise<void> {
  if (!question || answer === undefined || answer === null) {
    return;
  }

  let answerStr: string;
  if (Array.isArray(answer)) {
    answerStr = answer.join(', ');
  } else {
    answerStr = String(answer);
  }

  const questionTrimmed = question.trim();
  const answerTrimmed = answerStr.trim();

  if (!cyaAddressData.collectedQAPairs) cyaAddressData.collectedQAPairs = [];

  // Check for duplicates to prevent duplicate logging and data collection
  const isDuplicate = cyaAddressData.collectedQAPairs.some(
    pair => pair.question === questionTrimmed && pair.answer === answerTrimmed && pair.step === actionName
  );

  if (isDuplicate) {
    return; // Skip if already collected
  }

  cyaAddressData.collectedQAPairs.push({
    step: actionName,
    question: questionTrimmed,
    answer: answerTrimmed
  });

  console.log(`🏠 [Address CYA] Collected Q&A Pair:`, {
    step: actionName,
    question: questionTrimmed,
    answer: answerTrimmed
  });
}

/**
 * Collect CYA data for Final CYA (all pages after address pages - 35+ pages)
 *
 * @param actionName - Name of the action collecting this data (e.g., 'selectClaimantType')
 * @param question - Question text as it appears on CYA (from spec, page.data, or hardcoded)
 * @param answer - Answer value (string, number, array, etc. - will be converted to string)
 */
export async function collectCYAData(actionName: string, question: string | number | boolean | object | string[] | object[], answer: any): Promise<void> {
  if (!question || answer === undefined || answer === null) {
    return;
  }

  let answerStr: string;
  if (Array.isArray(answer)) {
    answerStr = answer.join(', ');
  } else {
    answerStr = String(answer);
  }

  const questionTrimmed = typeof question === 'string' ? question.trim() : String(question);
  const answerTrimmed = answerStr.trim();

  if (!cyaData.collectedQAPairs) cyaData.collectedQAPairs = [];

  // Check for duplicates to prevent duplicate logging and data collection
  const isDuplicate = cyaData.collectedQAPairs.some(
    pair => pair.question === questionTrimmed && pair.answer === answerTrimmed && pair.step === actionName
  );

  if (isDuplicate) {
    return; // Skip if already collected
  }

  cyaData.collectedQAPairs.push({
    step: actionName,
    question: questionTrimmed,
    answer: answerTrimmed
  });

  console.log(`📝 [Final CYA] Collected Q&A Pair:`, {
    step: actionName,
    question: questionTrimmed,
    answer: answerTrimmed
  });
}

