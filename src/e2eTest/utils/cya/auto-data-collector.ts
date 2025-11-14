/**
 * Auto Data Collector
 * 
 * Automatically collects journey data from actions by extracting questions
 * from page data, action parameters, or page elements.
 * 
 * Supports all data entry types:
 * - Radio buttons (single selection)
 * - Text inputs (free text, textareas)
 * - Checkboxes (multiple selections)
 * - Dropdowns/Selects (single selection from list)
 * - Date inputs (day/month/year)
 * - Time inputs (hour/minute/second)
 * - File uploads (document names)
 * - Address selection (formatted address)
 */

import { Page, Locator } from '@playwright/test';
import { JourneyDataCollector } from './journey-data-collector';
import { actionData, actionRecord } from '../interfaces';

export class AutoDataCollector {
  private collector: JourneyDataCollector;

  constructor() {
    this.collector = JourneyDataCollector.getInstance();
  }

  /**
   * Collect answer from a radio button selection
   * Extracts question from page data or action parameters
   */
  async collectRadioButtonAnswer(
    page: Page | null,
    actionName: string,
    selectedOption: string | actionRecord,
    questionText?: string,
    pageData?: any
  ): Promise<void> {
    let answer: string;
    if (typeof selectedOption === 'string') {
      answer = selectedOption;
    } else {
      const optionValue = selectedOption.option || selectedOption.value || '';
      answer = String(optionValue);
    }
    const question = await this.extractQuestion(page, actionName, questionText, pageData);
    
    if (question && answer) {
      this.collector.setAnswer(question, answer);
    }
  }

  /**
   * Collect answer from a text input field
   */
  async collectTextInputAnswer(
    page: Page | null,
    actionName: string,
    labelText: string | actionRecord,
    inputValue: string | number | boolean | object,
    pageData?: any
  ): Promise<void> {
    const label = typeof labelText === 'string' ? labelText : (String(labelText.text || labelText.label || ''));
    const question = await this.extractQuestion(page, actionName, label, pageData);
    
    if (question && inputValue) {
      this.collector.setAnswer(question, String(inputValue));
    }
  }

  /**
   * Collect answer from a dropdown/select field
   */
  async collectSelectAnswer(
    page: Page | null,
    actionName: string,
    labelText: string,
    selectedValue: string | number,
    pageData?: any
  ): Promise<void> {
    const question = await this.extractQuestion(page, actionName, labelText, pageData);
    
    if (question && selectedValue !== undefined && selectedValue !== null) {
      this.collector.setAnswer(question, String(selectedValue));
    }
  }

  /**
   * Collect answer from checkbox selection(s) - supports multiple selections
   */
  async collectCheckboxAnswer(
    page: Page | null,
    actionName: string,
    labelText: string | string[] | actionRecord,
    checkedOptions: string[] | actionRecord,
    pageData?: any
  ): Promise<void> {
    let question: string | undefined;
    let answers: string[] = [];

    // Handle different input formats
    if (Array.isArray(checkedOptions)) {
      answers = checkedOptions.map(opt => String(opt));
      const label = Array.isArray(labelText) ? labelText[0] : (typeof labelText === 'string' ? labelText : '');
      question = await this.extractQuestion(page, actionName, label, pageData);
    } else if (typeof checkedOptions === 'object' && checkedOptions !== null) {
      // Handle actionRecord format
      if ('option' in checkedOptions && Array.isArray(checkedOptions.option)) {
        answers = checkedOptions.option.map(opt => String(opt));
      } else if ('question' in checkedOptions && 'option' in checkedOptions) {
        question = String(checkedOptions.question || '');
        const optionValue = checkedOptions.option;
        answers = Array.isArray(optionValue) 
          ? optionValue.map(opt => String(opt))
          : [String(optionValue)];
      }
    }

    if (!question && typeof labelText === 'string') {
      question = await this.extractQuestion(page, actionName, labelText, pageData);
    }

    if (question && answers.length > 0) {
      this.collector.setAnswer(question, answers.join(', '));
    }
  }

  /**
   * Collect date input (day, month, year)
   */
  async collectDateAnswer(
    page: Page | null,
    actionName: string,
    day: string | number | undefined,
    month: string | number | undefined,
    year: string | number | undefined,
    questionText?: string,
    pageData?: any
  ): Promise<void> {
    if (day && month && year) {
      const question = await this.extractQuestion(page, actionName, questionText || 'Date', pageData);
      const formattedDate = `${day}/${month}/${year}`;
      
      if (question) {
        this.collector.setAnswer(question, formattedDate);
      }
    }
  }

  /**
   * Collect time input (hour, minute, second)
   */
  async collectTimeAnswer(
    page: Page | null,
    actionName: string,
    hour: string | number | undefined,
    minute: string | number | undefined,
    second: string | number | undefined,
    questionText?: string,
    pageData?: any
  ): Promise<void> {
    if (hour !== undefined && minute !== undefined && second !== undefined) {
      const question = await this.extractQuestion(page, actionName, questionText || 'Time', pageData);
      const formattedTime = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`;
      
      if (question) {
        this.collector.setAnswer(question, formattedTime);
      }
    }
  }

  /**
   * Collect file upload information
   */
  async collectFileUploadAnswer(
    page: Page | null,
    actionName: string,
    fileName: string | string[],
    questionText?: string,
    pageData?: any
  ): Promise<void> {
    const question = await this.extractQuestion(page, actionName, questionText || 'Uploaded documents', pageData);
    const files = Array.isArray(fileName) ? fileName : [fileName];
    
    if (question && files.length > 0) {
      this.collector.setAnswer(question, files.join(', '));
    }
  }

  /**
   * Collect multiple answers from an action record (for complex forms)
   */
  async collectMultipleAnswers(
    page: Page | null,
    actionName: string,
    answers: actionRecord,
    pageData?: any
  ): Promise<void> {
    for (const [key, value] of Object.entries(answers)) {
      // Skip internal fields
      if (key === 'question' || key === 'option' || key === 'text' || key === 'index') {
        continue;
      }

      // Try to find question text in page data
      const questionKey = this.findQuestionKeyInPageData(key, pageData);
      const question = questionKey || key;
      
      if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
        this.collector.setAnswer(question, String(value));
      } else if (Array.isArray(value)) {
        this.collector.setAnswer(question, value.join(', '));
      } else if (typeof value === 'object' && value !== null) {
        // Recursively collect nested objects
        await this.collectMultipleAnswers(page, actionName, value as actionRecord, pageData);
      }
    }
  }

  /**
   * Collect address information
   */
  async collectAddressAnswer(
    page: Page | null,
    address: { buildingStreet?: string; townCity?: string; postcode?: string } | string
  ): Promise<void> {
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
    
    this.collector.setAnswer(
      'What is the address of the property you\'re claiming possession of?',
      answer
    );
  }

  /**
   * Extract question text from various sources
   */
  private async extractQuestion(
    page: Page | null,
    actionName: string,
    explicitQuestion?: string,
    pageData?: any
  ): Promise<string | undefined> {
    // Priority 1: Explicit question text provided
    if (explicitQuestion) {
      return explicitQuestion;
    }

    // Priority 2: Extract from page data based on action name
    const questionFromPageData = this.getQuestionFromPageData(actionName, pageData);
    if (questionFromPageData) {
      return questionFromPageData;
    }

    // Priority 3: Try to extract from page element (label, heading, etc.)
    if (page) {
      return await this.extractQuestionFromPage(page, actionName).catch(() => undefined);
    }

    return undefined;
  }

  /**
   * Get question from page data based on action name and common patterns
   * Priority: 1. Direct CYA question keys in pageData, 2. Legacy mappings, 3. Direct pageData keys
   */
  private getQuestionFromPageData(actionName: string, pageData?: any): string | undefined {
    if (!pageData) return undefined;

    // Priority 1: Try to find CYA question directly in pageData based on actionName mapping
    const cyaQuestionKey = this.getCYAQuestionKey(actionName, pageData);
    if (cyaQuestionKey && pageData[cyaQuestionKey] && typeof pageData[cyaQuestionKey] === 'string') {
      return pageData[cyaQuestionKey];
    }

    // Priority 2: Legacy action name to page data key mappings
    const questionMappings: Record<string, string[]> = {
      'selectClaimantType': ['whoIsTheClaimantInThisCase', 'mainHeader'],
      'selectClaimType': ['isThisAClaimForPossessionOfProperty', 'mainHeader'],
      'selectClaimantName': ['isThisTheCorrectClaimantName', 'whatIsTheCorrectClaimantName', 'mainHeader'],
      'selectContactPreferences': [
        'doYouWantToUseThisEmailAddressForNotifications',
        'doYouWantDocumentsToBeSentToThisAddress',
        'doYouWantToProvideAContactPhoneNumber',
        'emailAddressForNotifications',
        'doYouWantDocumentsToBeSentToAddress',
        'provideContactPhoneNumber'
      ],
      'defendantDetails': [
        'doYouKnowTheDefendantsName',
        'doYouKnowTheDefendantsCorrespondenceAddress',
        'isTheDefendantsCorrespondenceAddressTheSame',
        'doYouKnowTheDefendantsEmailAddress',
        'doYouKnowTheDefendantName',
        'defendantCorrespondenceAddress',
        'isCorrespondenceAddressSame',
        'defendantEmailAddress'
      ],
      'selectTenancyOrLicenceDetails': [
        'whatTypeOfTenancyOrLicenceIsInPlace',
        'whenDidTheTenancyBegin',
        'mainHeader',
        'whatTypeOfTenancyOrLicence'
      ],
      'selectGroundsForPossession': [
        'areYouClaimingPossessionBecauseOfRentArrearsOrBreach',
        'mainHeader'
      ],
      'selectPreActionProtocol': [
        'haveYouFollowedThePreActionProtocol',
        'mainHeader'
      ],
      'selectMediationAndSettlement': [
        'haveYouAttemptedMediationWithTheDefendants',
        'haveYouTriedToReachASettlementWithTheDefendants',
        'attemptedMediationWithDefendants',
        'settlementWithDefendants'
      ],
      'selectNoticeOfYourIntention': ['haveYouServedNoticeToTheDefendants', 'mainHeader', 'servedNoticeInteractiveText'],
      'selectNoticeDetails': ['howDidYouServeTheNotice', 'howDidYouServeNotice'],
      'selectClaimantCircumstances': ['isThereAnyInformationYoudLikeToProvideAboutClaimantsCircumstances', 'mainHeader'],
      'selectDefendantCircumstances': ['isThereAnyInformationYoudLikeToProvideAboutDefendantsCircumstances', 'mainHeader'],
      'selectAlternativesToPossession': ['inTheAlternativeToPossessionWouldYouLikeToClaim', 'mainHeader'],
      'selectClaimingCosts': ['areYouClaimingCosts', 'mainHeader'],
      'selectAdditionalReasonsForPossession': ['isThereAnyOtherInformationYoudLikeToProvideAboutYourReasonsForPossession', 'mainHeader', 'additionalReasonsForPossessionLabel'],
      'selectUnderlesseeOrMortgageeEntitledToClaim': ['isThereAnUnderlesseeOrMortgageeEntitledToClaimReliefAgainstForfeiture', 'mainHeader', 'entitledToClaimRelief'],
      'wantToUploadDocuments': ['doYouWantToUploadAnyAdditionalDocuments', 'uploadAnyAdditionalDocumentsLabel'],
      'selectApplications': ['areYouMakingAnyApplications', 'mainHeader'],
      'selectLanguageUsed': ['whichLanguageDidYouUseToCompleteThisService', 'whichLanguageUsedQuestion'],
      'selectYourPossessionGrounds': ['whatAreYourGroundsForPossession', 'whatAreYourAdditionalGroundsForPossession', 'mainHeader'],
      'enterReasonForPossession': ['detailsAboutYourReason'],
      'provideRentDetails': ['howMuchIsTheRent', 'enterFrequency', 'HowMuchRentLabel', 'rentFrequencyLabel'],
      'selectDailyRentAmount': ['isTheAmountPerDayCorrect', 'mainHeader'],
      'provideDetailsOfRentArrears': ['totalRentArrears', 'forThePeriodShownOnTheRentStatement', 'totalRentArrearsLabel', 'periodShownOnRentStatementLabel'],
      'selectMoneyJudgment': ['areYouClaimingAMoneyJudgment', 'mainHeader'],
      'selectStatementOfExpressTerms': ['haveYouServedTheDefendantsWithAStatementOfTheExpressTerms', 'statementOfExpressTermsQuestion'],
      'enterReasonForSuspensionOrder': ['mainHeader'],
      'enterReasonForDemotionOrder': ['mainHeader'],
      'enterReasonForSuspensionAndDemotionOrder': ['mainHeader'],
      'selectUnderlesseeOrMortgageeDetails': ['nameQuestion', 'addressQuestion', 'anotherUnderlesseeOrMortgageeQuestion']
    };

    const keys = questionMappings[actionName];
    if (keys) {
      // Try each key in order
      for (const key of keys) {
        const value = this.getNestedValue(pageData, key);
        if (value && typeof value === 'string') {
          return value;
        }
      }
    }

    return undefined;
  }

  /**
   * Get CYA question key directly from pageData based on action name
   */
  private getCYAQuestionKey(actionName: string, pageData: any): string | undefined {
    if (!pageData || typeof pageData !== 'object') return undefined;

    // Map action names to likely CYA question keys (now directly in pageData)
    const actionToCYAKey: Record<string, string[]> = {
      'selectClaimantType': ['whoIsTheClaimantInThisCase'],
      'selectClaimType': ['isThisAClaimForPossessionOfProperty'],
      'selectClaimantName': ['isThisTheCorrectClaimantName', 'whatIsTheCorrectClaimantName'],
      'selectContactPreferences': [
        'doYouWantToUseThisEmailAddressForNotifications',
        'doYouWantDocumentsToBeSentToThisAddress',
        'doYouWantToProvideAContactPhoneNumber'
      ],
      'defendantDetails': [
        'doYouKnowTheDefendantsName',
        'doYouKnowTheDefendantsCorrespondenceAddress',
        'isTheDefendantsCorrespondenceAddressTheSame',
        'doYouKnowTheDefendantsEmailAddress'
      ],
      'selectTenancyOrLicenceDetails': [
        'whatTypeOfTenancyOrLicenceIsInPlace',
        'whenDidTheTenancyBegin'
      ],
      'selectGroundsForPossession': ['areYouClaimingPossessionBecauseOfRentArrearsOrBreach'],
      'selectPreActionProtocol': ['haveYouFollowedThePreActionProtocol']
    };

    const possibleKeys = actionToCYAKey[actionName];
    if (!possibleKeys) return undefined;

    // Try each key in order
    for (const key of possibleKeys) {
      if (key in pageData && typeof pageData[key] === 'string') {
        return key;
      }
    }

    return undefined;
  }

  /**
   * Find question key in page data by matching property names
   */
  private findQuestionKeyInPageData(key: string, pageData?: any): string | undefined {
    if (!pageData) return undefined;

    // Try exact match
    if (pageData[key] && typeof pageData[key] === 'string') {
      return pageData[key];
    }

    // Try case-insensitive match
    const lowerKey = key.toLowerCase();
    for (const [pageKey, value] of Object.entries(pageData)) {
      if (pageKey.toLowerCase() === lowerKey && typeof value === 'string') {
        return value;
      }
    }

    // Try partial match (contains)
    for (const [pageKey, value] of Object.entries(pageData)) {
      if (pageKey.toLowerCase().includes(lowerKey) || lowerKey.includes(pageKey.toLowerCase())) {
        if (typeof value === 'string' && value.includes('?')) {
          return value;
        }
      }
    }

    return undefined;
  }

  /**
   * Get nested value from object using dot notation or direct key
   */
  private getNestedValue(obj: any, path: string): any {
    if (!obj) return undefined;
    
    // Direct key
    if (obj[path] !== undefined) {
      return obj[path];
    }

    // Try dot notation
    const parts = path.split('.');
    let current = obj;
    for (const part of parts) {
      if (current && typeof current === 'object' && part in current) {
        current = current[part];
      } else {
        return undefined;
      }
    }
    return current;
  }

  /**
   * Extract question from page element (label, heading, etc.)
   */
  private async extractQuestionFromPage(page: Page, actionName: string): Promise<string | undefined> {
    try {
      // Try to find label associated with the action
      const label = page.locator('label, .govuk-label, [class*="label"]').first();
      const isVisible = await label.isVisible({ timeout: 1000 }).catch(() => false);
      
      if (isVisible) {
        const text = await label.textContent();
        if (text && text.trim()) {
          return text.trim();
        }
      }

      // Try heading
      const heading = page.locator('h1, h2, .govuk-heading-l, .govuk-heading-m').first();
      const headingVisible = await heading.isVisible({ timeout: 1000 }).catch(() => false);
      
      if (headingVisible) {
        const text = await heading.textContent();
        if (text && text.trim()) {
          return text.trim();
        }
      }
    } catch {
      // Ignore errors
    }

    return undefined;
  }
}

// Singleton instance
let autoCollectorInstance: AutoDataCollector | null = null;

export function getAutoCollector(): AutoDataCollector {
  if (!autoCollectorInstance) {
    autoCollectorInstance = new AutoDataCollector();
  }
  return autoCollectorInstance;
}
