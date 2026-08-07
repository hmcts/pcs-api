import {expect, Page} from '@playwright/test';

import { confirmIfTheseDocumentsRelateToAnApplication } from "@data/page-data-figma/page-data-legalRepresentative";
import { performAction, performValidation } from '../../controller';
import { IAction, actionRecord } from '../../interfaces';
import {uploadAdditionalDocumentsInformation} from "@data/page-data-figma/page-data-legalRepresentative";
import {getCaseTypeId} from "@utils/common/caseType.utils";
import {VERY_LONG_TIMEOUT} from "../../../playwright.config";
import {home} from "@data/page-data";

export class DocumentsAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['uploadAdditionalDocumentsInfo', () => this.uploadAdditionalDocumentsInfo()],
      ['navigateToSummaryPage', () => this.navigateToSummaryPage(page)],

      [
        'verifyDocumentRelatesToApplication',
        () => this.verifyDocumentRelatesToApplication(page, fieldName as actionRecord),
      ],
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }
    await actionToPerform();
  }

  private async uploadAdditionalDocumentsInfo(): Promise<void> {
    await performAction('clickButton', uploadAdditionalDocumentsInformation.continueButton);
  }


  private async verifyDocumentRelatesToApplication(page: Page, confirmDocumentData: actionRecord) {
    await performValidation('text', {
      elementType: 'paragraph',
      text: confirmIfTheseDocumentsRelateToAnApplication.weUsuallyParagraph,
    });
    await performValidation('text', {
      elementType: 'paragraph',
      text: confirmIfTheseDocumentsRelateToAnApplication.ifYourApplicationParagraph,
    });

    const formattedDate = new Intl.DateTimeFormat('en-GB', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    })
      .format(new Date())
      .replace(',', '');

    const expectedOptions: string[] = [];
    const optionText = `${confirmDocumentData.option} ${formattedDate}`;

    // repeat the primary option once per application (e.g. once per defendant that submitted one)
    const repeatCount = Number(confirmDocumentData.count ?? 1);
    for (let i = 0; i < repeatCount; i++) {
      expectedOptions.push(optionText);
    }

    // optional 2nd distinct option (if a different application type was also submitted)
    if (confirmDocumentData.previousApplicationOption) {
      expectedOptions.push(`${confirmDocumentData.previousApplicationOption} ${formattedDate}`);
    }

    // fixed "No" option, always last
    expectedOptions.push(confirmIfTheseDocumentsRelateToAnApplication.noRadioOption);

    console.log('Expected radio order:', expectedOptions);

    const radioLabels = page.locator('.govuk-radios__label, label.form-label');
    for (let i = 0; i < expectedOptions.length; i++) {
      const actualText = ((await radioLabels.nth(i).textContent()) ?? '').replace(/\s+/g, ' ').trim();
      const expectedText = expectedOptions[i].replace(/\s+/g, ' ').trim();

      console.log(`Radio ${i}:`, actualText);
      console.log(`Expected ${i}:`, expectedText);

      if (!actualText.includes(expectedText)) {
        throw new Error(
          `Radio order mismatch at index ${i}.\nExpected: "${expectedText}"\nActual: "${actualText}"`
        );
      }
    }

    const selectOption =
      confirmDocumentData.option === confirmIfTheseDocumentsRelateToAnApplication.noRadioOption
        ? confirmDocumentData.option
        : optionText;

// Find which radio should be selected
    const radioIndex = expectedOptions.findIndex(
      option =>
        option.replace(/\s+/g, ' ').trim() ===
        selectOption.replace(/\s+/g, ' ').trim()
    );

    if (radioIndex === -1) {
      throw new Error(`Could not find radio option: ${selectOption}`);
    }

    await page.locator('input[type="radio"]').nth(radioIndex).check();

    await performAction(
      'clickButton',
      confirmIfTheseDocumentsRelateToAnApplication.continueButton
    );
  }

  private async navigateToSummaryPage(page: Page) {
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    await expect(async () => {
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`, { waitUntil: 'domcontentloaded' });
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
    await page.waitForLoadState();
    await page.locator('.spinner-container').waitFor({ state: 'detached' });
    await performValidation('mainHeader', home.caseSummary);
  }
}
