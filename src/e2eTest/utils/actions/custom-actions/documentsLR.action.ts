import { Page } from '@playwright/test';

import { confirmIfTheseDocumentsRelateToAnApplication } from "@data/page-data-figma/page-data-legalRepresentative";
import { performAction, performValidation } from '../../controller';
import { IAction, actionRecord } from '../../interfaces';
import {uploadAdditionalDocumentsInformation} from "@data/page-data-figma/page-data-legalRepresentative";

export class DocumentsAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['uploadAdditionalDocumentsInfo', () => this.uploadAdditionalDocumentsInfo()],
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
    // 1st radio -> option
    const optionText = `${confirmDocumentData.option} ${formattedDate}`;
    expectedOptions.push(optionText);

    // 2nd radio -> previousApplicationOption (if passed)
    if (confirmDocumentData.previousApplicationOption) {
      expectedOptions.push(`${confirmDocumentData.previousApplicationOption} ${formattedDate}`);
    }

    // 3rd radio -> noOption (always visible)
    expectedOptions.push(confirmIfTheseDocumentsRelateToAnApplication.noRadioOption);

    console.log('Expected radio order:', expectedOptions);

    // Verify UI order
    const radioLabels = page.locator('.govuk-radios__label');
    for (let i = 0; i < expectedOptions.length; i++) {
      const actualText = ((await radioLabels.nth(i).textContent()) ?? '').replace(/\s+/g, ' ').trim();

      const expectedText = expectedOptions[i].replace(/\s+/g, ' ').trim();

      console.log(`Radio ${i}:`, actualText);
      console.log(`Expected ${i}:`, expectedText);

      if (!actualText.includes(expectedText)) {
        throw new Error(
          `Radio order mismatch at index ${i}.
Expected: "${expectedText}"
Actual: "${actualText}"`
        );
      }
    }

    const selectOption =
      confirmDocumentData.option === confirmIfTheseDocumentsRelateToAnApplication.noRadioOption
        ? confirmDocumentData.option
        : `${confirmDocumentData.option} ${formattedDate}`;

    // VERIFY option is visible on UI BEFORE clicking
    await performValidation('elementToBeVisible', { elementType: 'radio', text: selectOption });
    await performAction('clickRadioButton', {
      question: confirmDocumentData.question,
      option: selectOption,
    });
    await performAction('clickButton', confirmIfTheseDocumentsRelateToAnApplication.continueButton);
  }
}
