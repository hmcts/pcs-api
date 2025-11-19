import { Page, test } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaData } from '@utils/data/cya-data';

/**
 * Validation class for Check Your Answers (CYA) page
 * Validates that all questions and answers displayed on the CYA page match the data collected during the journey
 */
export class CheckYourAnswersValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    await test.step('Validate Check Your Answers page', async () => {
      // Wait for page to stabilize after navigation
      await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
      await page.waitForTimeout(1000); // Additional wait for dynamic content

      // Wait for CYA page to load - try multiple possible selectors
      const possibleSelectors = [
        'h1:has-text("Check your answers")',
        'h1:has-text("Check Your Answers")',
        'h1:has-text("Check Your answers")',
        '.govuk-heading-l:has-text("Check your answers")',
        '.govuk-heading-l:has-text("Check Your Answers")',
        '[class*="check-your-answers"]',
        '.case-viewer-label',
        'button:has-text("Save and continue")',
        'a:has-text("Save and continue")'
      ];

      let pageLoaded = false;
      for (const selector of possibleSelectors) {
        try {
          await page.waitForSelector(selector, { timeout: 3000 });
          pageLoaded = true;
          break;
        } catch (e) {
          // Try next selector
        }
      }

      if (!pageLoaded) {
        // Log current page info for debugging
        const currentUrl = page.url();
        const pageTitle = await page.title();
        const h1Text = await page.locator('h1').first().textContent().catch(() => 'N/A');
        const pageContent = await page.content();
        const hasCheckText = pageContent.toLowerCase().includes('check');
        const hasSaveContinue = pageContent.toLowerCase().includes('save and continue') || pageContent.toLowerCase().includes('save and continue');

        throw new Error(
          `CYA page not found.\n` +
          `Current URL: ${currentUrl}\n` +
          `Page Title: ${pageTitle}\n` +
          `H1 Text: ${h1Text}\n` +
          `Page contains 'check': ${hasCheckText}\n` +
          `Page contains 'save and continue': ${hasSaveContinue}`
        );
      }

      // Extract all questions and answers from the CYA page
      const cyaPageData = await this.extractCYAPageData(page);

      // Log extracted data for debugging
      if (cyaPageData.size === 0) {
        const pageContent = await page.content();
        throw new Error(`No CYA data extracted from page. Page content length: ${pageContent.length}`);
      }

      // Validate collected data against page data
      await this.validateCYAData(cyaPageData);
    });
  }

  /**
   * Extract all questions and answers from the CYA page
   */
  private async extractCYAPageData(page: Page): Promise<Map<string, string>> {
    const cyaDataMap = new Map<string, string>();

    // Find all summary list rows (standard GOV.UK pattern for CYA pages)
    const summaryRows = page.locator('.govuk-summary-list__row');
    const rowCount = await summaryRows.count();

    for (let i = 0; i < rowCount; i++) {
      const row = summaryRows.nth(i);
      const question = await row.locator('.govuk-summary-list__key').textContent();
      const answer = await row.locator('.govuk-summary-list__value').textContent();

      if (question && answer) {
        cyaDataMap.set(question.trim(), answer.trim());
      }
    }

    // Also check for any table-based CYA layouts
    const tableRows = page.locator('table tr');
    const tableRowCount = await tableRows.count();

    for (let i = 0; i < tableRowCount; i++) {
      const row = tableRows.nth(i);
      const cells = row.locator('td, th');
      const cellCount = await cells.count();

      if (cellCount >= 2) {
        const question = await cells.nth(0).textContent();
        const answer = await cells.nth(1).textContent();

        if (question && answer && question.trim() && answer.trim()) {
          cyaDataMap.set(question.trim(), answer.trim());
        }
      }
    }

    // Check for case viewer format (CCD pattern)
    const caseViewerLabels = page.locator('.case-viewer-label, .case-field__label');
    const labelCount = await caseViewerLabels.count();

    for (let i = 0; i < labelCount; i++) {
      const label = caseViewerLabels.nth(i);
      const question = await label.textContent();

      // Try to find the corresponding value
      const valueLocator = label.locator('xpath=../following-sibling::td[1]').or(
        label.locator('xpath=../../td[2]')
      ).or(
        page.locator(`.case-viewer-label:has-text("${question}")`).locator('xpath=../..').locator('.text-16 span')
      );

      const answer = await valueLocator.first().textContent().catch(() => null);

      if (question && answer && question.trim() && answer.trim()) {
        cyaDataMap.set(question.trim(), answer.trim());
      }
    }

    return cyaDataMap;
  }

  /**
   * Validate collected CYA data against the page data
   */
  private async validateCYAData(cyaPageData: Map<string, string>): Promise<void> {
    const errors: string[] = [];

    // Validate property address - check Building and Street, Town or City, Postcode separately
    if (cyaData.propertyAddress) {
      await this.validateField(cyaPageData, 'Building and Street', cyaData.propertyAddress.buildingStreet || '', errors);
      await this.validateField(cyaPageData, 'Town or City', cyaData.propertyAddress.townCity || '', errors);
      await this.validateField(cyaPageData, 'Postcode', cyaData.propertyAddress.postcode || '', errors);
    }

    // Validate claimant type
    if (cyaData.claimantType) {
      await this.validateField(cyaPageData, 'Who is the claimant in this case?', cyaData.claimantType, errors);
    }

    // Validate contact preferences
    if (cyaData.contactPreferences) {
      if (cyaData.contactPreferences.emailNotifications) {
        await this.validateField(cyaPageData, 'Do you want to use this email address for notifications?', cyaData.contactPreferences.emailNotifications, errors);
      }
      if (cyaData.contactPreferences.correspondenceAddress) {
        await this.validateField(cyaPageData, 'Do you want documents to be sent to this address?', cyaData.contactPreferences.correspondenceAddress, errors);
      }
      if (cyaData.contactPreferences.phoneNumber) {
        await this.validateField(cyaPageData, 'Do you want to provide a contact phone number?', cyaData.contactPreferences.phoneNumber, errors);
      }
    }

    // Validate defendant details
    if (cyaData.defendantDetails) {
      if (cyaData.defendantDetails.firstName) {
        await this.validateField(cyaPageData, "Defendant's first name", cyaData.defendantDetails.firstName, errors);
      }
      if (cyaData.defendantDetails.lastName) {
        await this.validateField(cyaPageData, "Defendant's last name", cyaData.defendantDetails.lastName, errors);
      }
      if (cyaData.defendantDetails.correspondenceAddress) {
        await this.validateField(cyaPageData, "Is the defendant's correspondence address the same as the address of the property you're claiming possession of?", cyaData.defendantDetails.correspondenceAddress, errors);
      }
    }

    // Validate tenancy/licence details
    if (cyaData.tenancyLicenceDetails) {
      if (cyaData.tenancyLicenceDetails.type) {
        await this.validateField(cyaPageData, 'What type of tenancy or licence is in place?', cyaData.tenancyLicenceDetails.type, errors);
      }
      if (cyaData.tenancyLicenceDetails.startDate) {
        // Convert date format from DD/MM/YYYY to DD MMM YYYY (e.g., "16/04/2021" -> "16 Apr 2021")
        const formattedDate = this.formatDateForCYA(cyaData.tenancyLicenceDetails.startDate);
        await this.validateField(cyaPageData, 'What date did the tenancy or licence begin?', formattedDate, errors);
      }
    }

    // Validate grounds for possession
    if (cyaData.groundsForPossession) {
      if (cyaData.groundsForPossession.rentArrearsGrounds && cyaData.groundsForPossession.rentArrearsGrounds.length > 0) {
        await this.validateField(cyaPageData, 'What are your grounds for possession?', cyaData.groundsForPossession.rentArrearsGrounds.join(','), errors);
      }
      if (cyaData.groundsForPossession.mandatoryGrounds && cyaData.groundsForPossession.mandatoryGrounds.length > 0) {
        await this.validateField(cyaPageData, 'Mandatory grounds', cyaData.groundsForPossession.mandatoryGrounds.join(','), errors);
      }
      if (cyaData.groundsForPossession.discretionaryGrounds && cyaData.groundsForPossession.discretionaryGrounds.length > 0) {
        await this.validateField(cyaPageData, 'Discretionary grounds', cyaData.groundsForPossession.discretionaryGrounds.join(','), errors);
      }
    }

    // Validate pre-action protocol
    if (cyaData.preActionProtocol) {
      await this.validateField(cyaPageData, 'Have you followed the pre-action protocol?', cyaData.preActionProtocol, errors);
    }

    // Validate mediation and settlement
    if (cyaData.mediationAndSettlement) {
      if (cyaData.mediationAndSettlement.attemptedMediation) {
        await this.validateField(cyaPageData, 'Have you attempted mediation with the defendants?', cyaData.mediationAndSettlement.attemptedMediation, errors);
      }
      if (cyaData.mediationAndSettlement.settlement) {
        await this.validateField(cyaPageData, 'Have you tried to reach a settlement with the defendants?', cyaData.mediationAndSettlement.settlement, errors);
      }
    }

    // Validate notice details
    if (cyaData.noticeDetails) {
      if (cyaData.noticeDetails.servedNotice) {
        await this.validateField(cyaPageData, 'Have you served notice to the defendants?', cyaData.noticeDetails.servedNotice, errors);
      }
      if (cyaData.noticeDetails.howServed) {
        await this.validateField(cyaPageData, 'How did you serve the notice?', cyaData.noticeDetails.howServed, errors);
      }
      if (cyaData.noticeDetails.serviceDate) {
        // Convert date format from DD/MM/YYYY to DD MMM YYYY
        const formattedDate = this.formatDateForCYA(cyaData.noticeDetails.serviceDate);
        await this.validateField(cyaPageData, 'Date the document was posted', formattedDate, errors);
      }
    }

    // Validate rent details
    if (cyaData.rentDetails) {
      if (cyaData.rentDetails.amount) {
        // Format amount to include currency symbol if needed
        const formattedAmount = cyaData.rentDetails.amount.startsWith('£')
          ? cyaData.rentDetails.amount
          : `£${parseFloat(cyaData.rentDetails.amount).toFixed(2)}`;
        await this.validateField(cyaPageData, 'How much is the rent?', formattedAmount, errors);
      }
      if (cyaData.rentDetails.frequency) {
        await this.validateField(cyaPageData, 'How frequently should rent be paid?', cyaData.rentDetails.frequency, errors);
      }
      if (cyaData.rentDetails.dailyAmount) {
        await this.validateField(cyaPageData, 'Daily rent amount', cyaData.rentDetails.dailyAmount, errors);
      }
    }

    // Validate money judgment
    if (cyaData.moneyJudgment) {
      await this.validateField(cyaPageData, 'Money judgment', cyaData.moneyJudgment, errors);
    }

    // Validate claiming costs
    if (cyaData.claimingCosts) {
      await this.validateField(cyaPageData, 'Claiming costs', cyaData.claimingCosts, errors);
    }

    // Validate applications
    if (cyaData.applications) {
      await this.validateField(cyaPageData, 'Applications', cyaData.applications, errors);
    }

    // If there are errors, throw with details
    if (errors.length > 0) {
      throw new Error(`CYA validation failed:\n${errors.join('\n')}`);
    }
  }

  /**
   * Validate a single field
   */
  private async validateField(
    cyaPageData: Map<string, string>,
    questionKey: string,
    expectedValue: string,
    errors: string[]
  ): Promise<void> {
    // Try to find the question in the page data (case-insensitive, partial match)
    let found = false;
    let actualValue = '';
    let matchedQuestion = '';

    // Create multiple search patterns for the question
    const searchPatterns = [
      questionKey.toLowerCase(),
      questionKey.toLowerCase().replace(/\s+/g, ' '), // Normalize whitespace
      questionKey.toLowerCase().replace(/[^a-z0-9\s]/g, ''), // Remove special chars
    ];

    for (const [question, answer] of cyaPageData.entries()) {
      const normalizedQuestion = question.toLowerCase().trim();

      // Check if any search pattern matches
      for (const pattern of searchPatterns) {
        if (normalizedQuestion.includes(pattern) || pattern.includes(normalizedQuestion)) {
          found = true;
          actualValue = answer;
          matchedQuestion = question;
          break;
        }
      }

      if (found) break;
    }

    if (!found) {
      // Try fuzzy matching - check if any question contains key words
      const keyWords = questionKey.toLowerCase().split(/\s+/).filter(w => w.length > 3);
      for (const [question, answer] of cyaPageData.entries()) {
        const normalizedQuestion = question.toLowerCase();
        const matchCount = keyWords.filter(word => normalizedQuestion.includes(word)).length;
        if (matchCount >= Math.ceil(keyWords.length / 2)) {
          found = true;
          actualValue = answer;
          matchedQuestion = question;
          break;
        }
      }
    }

    if (!found) {
      errors.push(`Question "${questionKey}" not found on CYA page`);
      return;
    }

    // Normalize values for comparison (remove extra whitespace, handle case)
    const normalizedExpected = expectedValue.trim().toLowerCase();
    const normalizedActual = actualValue.trim().toLowerCase();

    // For array values (like grounds), check if expected is contained in actual
    if (normalizedExpected.includes(',') || normalizedActual.includes(',')) {
      // Split and compare individual items
      const expectedItems = normalizedExpected.split(',').map(s => s.trim()).filter(s => s);
      const actualItems = normalizedActual.split(',').map(s => s.trim()).filter(s => s);

      const allFound = expectedItems.every(item =>
        actualItems.some(actual => actual.includes(item) || item.includes(actual))
      );

      if (!allFound) {
        errors.push(`Question "${matchedQuestion}" (searched as "${questionKey}"): Expected contains "${expectedValue}", but found "${actualValue}"`);
      }
    } else {
      // Check if actual value contains expected value or vice versa (for partial matches)
      if (!normalizedActual.includes(normalizedExpected) && !normalizedExpected.includes(normalizedActual)) {
        errors.push(`Question "${matchedQuestion}" (searched as "${questionKey}"): Expected "${expectedValue}", but found "${actualValue}"`);
      }
    }
  }

  /**
   * Format date from DD/MM/YYYY to DD MMM YYYY format (e.g., "16/04/2021" -> "16 Apr 2021")
   */
  private formatDateForCYA(dateStr: string): string {
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    // Handle DD/MM/YYYY format
    const parts = dateStr.split('/');
    if (parts.length === 3) {
      const day = parts[0].padStart(2, '0');
      const month = months[parseInt(parts[1]) - 1];
      const year = parts[2];
      return `${day} ${month} ${year}`;
    }

    // If already in correct format or other format, return as is
    return dateStr;
  }
}

