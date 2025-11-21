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

  if (!cyaAddressData.collectedQAPairs) cyaAddressData.collectedQAPairs = [];
  cyaAddressData.collectedQAPairs.push({
    step: actionName,
    question: question.trim(),
    answer: answerStr.trim(),
    timestamp: new Date().toISOString()
  });
}

/**
 * Collect CYA data for Final CYA (all pages after address pages - 35+ pages)
 *
 * @param actionName - Name of the action collecting this data (e.g., 'selectClaimantType')
 * @param question - Question text as it appears on CYA (from spec, page.data, or hardcoded)
 * @param answer - Answer value (string, number, array, etc. - will be converted to string)
 */
export async function collectCYAData(actionName: string, question: string, answer: any): Promise<void> {
  if (!question || answer === undefined || answer === null) {
    return;
  }

  let answerStr: string;
  if (Array.isArray(answer)) {
    answerStr = answer.join(', ');
  } else {
    answerStr = String(answer);
  }

  if (!cyaData.collectedQAPairs) cyaData.collectedQAPairs = [];
  cyaData.collectedQAPairs.push({
    step: actionName,
    question: question.trim(),
    answer: answerStr.trim(),
    timestamp: new Date().toISOString()
  });
}

