/**
 * Journey Data Helpers
 *
 * Helper functions to collect answers from common actions.
 * These can be used to automatically collect data as the test progresses.
 */

import { Page } from '@playwright/test';
import {JourneyDataCollector} from "@utils/cya/journey-data-collector";

/**
 * Collect answer from a radio button selection
 */
export async function collectRadioButtonAnswer(
  page: Page,
  question: string,
  selectedOption: string
): Promise<void> {
  const collector = JourneyDataCollector.getInstance();
  collector.setAnswer(question, selectedOption);
}

/**
 * Collect answer from a text input
 */
export async function collectTextInputAnswer(
  page: Page,
  question: string,
  inputValue: string
): Promise<void> {
  const collector = JourneyDataCollector.getInstance();
  collector.setAnswer(question, inputValue);
}

/**
 * Collect answer from a dropdown/select
 */
export async function collectSelectAnswer(
  page: Page,
  question: string,
  selectedValue: string
): Promise<void> {
  const collector = JourneyDataCollector.getInstance();
  collector.setAnswer(question, selectedValue);
}

/**
 * Collect answer from checkbox selection
 */
export async function collectCheckboxAnswer(
  page: Page,
  question: string,
  checked: boolean
): Promise<void> {
  const collector = JourneyDataCollector.getInstance();
  collector.setAnswer(question, checked ? 'Yes' : 'No');
}

/**
 * Collect address information
 */
export async function collectAddressAnswer(
  page: Page,
  question: string,
  address: { buildingStreet?: string; townCity?: string; postcode?: string } | string
): Promise<void> {
  const collector = JourneyDataCollector.getInstance();

  let answer: string;
  if (typeof address === 'string') {
    answer = address;
  } else {
    const parts: string[] = [];
    if (address.buildingStreet) parts.push(address.buildingStreet);
    if (address.townCity) parts.push(address.townCity);
    if (address.postcode) parts.push(address.postcode);
    answer = parts.join(', ');
  }

  collector.setAnswer(question, answer);
}

/**
 * Collect multiple checkbox answers (for multi-select scenarios)
 */
export async function collectMultipleAnswers(
  page: Page,
  question: string,
  answers: string[]
): Promise<void> {
  const collector = JourneyDataCollector.getInstance();
  collector.setAnswer(question, answers.join(', '));
}

/**
 * Map common action data to CYA question format
 * This helps standardize question text between actions and CYA page
 */
export const questionMappings: Record<string, string> = {
  // Address
  'What is the address of the property you\'re claiming possession of?': 'What is the address of the property you\'re claiming possession of?',
  'Property address': 'What is the address of the property you\'re claiming possession of?',

  // Claimant
  'Who is the claimant in this case?': 'Who is the claimant in this case?',
  'Who are you making a possession claim against?': 'Who are you making a possession claim against?',
  'Is this the correct claimant name?': 'Is this the correct claimant name?',

  // Contact preferences
  'Do you want to use this email address for notifications?': 'Do you want to use this email address for notifications?',
  'Do you want documents to be sent to this address?': 'Do you want documents to be sent to this address?',
  'Do you want to provide a contact phone number?': 'Do you want to provide a contact phone number?',

  // Defendant
  'Do you know the defendant\'s name?': 'Do you know the defendant\'s name?',
  'Defendant\'s first name': 'Defendant\'s first name',
  'Is the defendant\'s correspondence address the same as the address of the property you\'re claiming possession of?': 'Is the defendant\'s correspondence address the same as the address of the property you\'re claiming possession of?',

  // Tenancy
  'What type of tenancy or licence is in place?': 'What type of tenancy or licence is in place?',
  'When did the tenancy begin?': 'When did the tenancy begin? (Optional)',

  // Grounds
  'What are your grounds for possession?': 'What are your grounds for possession?',
  'Reasons for possession': 'Reasons for possession',

  // Protocol
  'Pre-action protocol': 'Pre-action protocol',
  'Mediation and settlement': 'Mediation and settlement',

  // Notice
  'Notice of your intention': 'Notice of your intention',

  // Circumstances
  'Claimant circumstances': 'Claimant circumstances',
  'Defendant circumstances': 'Defendant circumstances',

  // Alternatives
  'Alternatives to possession': 'Alternatives to possession',

  // Costs
  'Claiming costs': 'Claiming costs',

  // Additional reasons
  'Additional reasons for possession': 'Additional reasons for possession',

  // Underlessee
  'Underlessee or mortgagee entitled to claim': 'Underlessee or mortgagee entitled to claim',

  // Documents
  'Do you want to upload any additional documents?': 'Do you want to upload any additional documents?',

  // Applications
  'Applications': 'Applications',

  // Language
  'Which language was used?': 'Which language was used?',
};

/**
 * Get standardized question text
 */
export function getStandardizedQuestion(question: string): string {
  return questionMappings[question] || question;
}

