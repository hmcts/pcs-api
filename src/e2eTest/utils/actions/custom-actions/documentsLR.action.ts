import {expect, Page} from '@playwright/test';
import { getFormattedDate } from "@utils/common/string.utils";

import {
  confirmIfTheseDocumentsRelateToAnApplication,
  uploadYourDocuments
} from "@data/page-data-figma/page-data-legalRepresentative";
import {performAction, performActions, performValidation} from '../../controller';
import { IAction, actionRecord } from '../../interfaces';
import {uploadAdditionalDocumentsInformation} from "@data/page-data-figma/page-data-legalRepresentative";
import {getCaseTypeId} from "@utils/common/caseType.utils";
import {VERY_LONG_TIMEOUT} from "../../../playwright.config";
import {home} from "@data/page-data";
import {caseInfo} from "@utils/actions/custom-actions/createCaseAPI.action";
import {createCaseApiData} from "@data/api-data";

export const documentsAddressInfo = {
  buildingStreet: createCaseApiData.createCasePayload.propertyAddress.AddressLine1,
  addressLine2: createCaseApiData.createCasePayload.propertyAddress.AddressLine2,
  townCity: createCaseApiData.createCasePayload.propertyAddress.PostTown,
  engOrWalPostcode: createCaseApiData.createCasePayload.propertyAddress.PostCode
};

export class DocumentsAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['uploadAdditionalDocumentsInfo', () => this.uploadAdditionalDocumentsInfo()],
      ['navigateToSummaryPage', () => this.navigateToSummaryPage(page)],
      ['uploadFiles', () => this.uploadFiles(page, fieldName as actionRecord)],
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


  private async verifyDocumentRelatesToApplication(
    page: Page,
    confirmDocumentData: actionRecord
  ) {
    await performValidation('text', {
      elementType: 'paragraph',
      text: confirmIfTheseDocumentsRelateToAnApplication.weUsuallyParagraph,
    });

    await performValidation('text', {
      elementType: 'paragraph',
      text: confirmIfTheseDocumentsRelateToAnApplication.ifYourApplicationParagraph,
    });

   const formattedDate = getFormattedDate();

    const expectedOptions: string[] = [
      `${confirmDocumentData.option} ${formattedDate}`,
    ];

    if (confirmDocumentData.previousApplicationOption) {
      expectedOptions.push(
        `${confirmDocumentData.previousApplicationOption} ${formattedDate}`
      );
    }

    expectedOptions.push(
      confirmIfTheseDocumentsRelateToAnApplication.noRadioOption
    );

    const radioLabels = page.locator(
      'input[type="radio"] + label.form-label'
    );

    await radioLabels.first().waitFor({
      state: 'visible',
      timeout: 10000,
    });

    const actualOptions = (await radioLabels.allTextContents()).map((text) =>
      text.replace(/\s+/g, ' ').trim()
    );

    // Verify expected options exist in the correct order
    let lastFoundIndex = -1;

    for (const expectedOption of expectedOptions) {
      const normalizedExpected = expectedOption
        .replace(/\s+/g, ' ')
        .trim();

      const foundIndex = actualOptions.findIndex(
        (actualOption, index) =>
          index > lastFoundIndex &&
          actualOption.includes(normalizedExpected)
      );

      if (foundIndex === -1) {
        throw new Error(
          `Radio option not found or is in the wrong order.\n` +
          `Expected: "${normalizedExpected}"\n` +
          `Actual options:\n${actualOptions
            .map((option, index) => `${index}: ${option}`)
            .join('\n')}`
        );
      }

      lastFoundIndex = foundIndex;
    }

    const selectOption =
      confirmDocumentData.option ===
      confirmIfTheseDocumentsRelateToAnApplication.noRadioOption
        ? confirmDocumentData.option
        : `${confirmDocumentData.option} ${formattedDate}`;

    const radioToSelect = radioLabels
      .filter({
        hasText: selectOption,
      })
      .first();

    await radioToSelect.waitFor({
      state: 'visible',
      timeout: 10000,
    });

    await radioToSelect.click();

    const selectedRadio = page.locator(
      'input[type="radio"]:checked'
    );

    await selectedRadio.waitFor({
      state: 'attached',
      timeout: 5000,
    });

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

  private async uploadFiles(page: Page, documentsData: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: '+ caseInfo.fid});
    await performValidation('text', { elementType: 'paragraph', text: `Property address: ${documentsAddressInfo.buildingStreet}, ${documentsAddressInfo.townCity}, ${documentsAddressInfo.engOrWalPostcode}`});

    if (Array.isArray(documentsData.documents)) {
      for (let fileIndex = 0; fileIndex < documentsData.documents.length; fileIndex++) {
        const document = documentsData.documents[fileIndex];

        // Removed manual "Add new" click — uploadFile likely already handles this
        await performActions(
          'Add Document',
          ['uploadFile', document.fileName],
        );

        const typeDropdown = page.locator(
          `[id^="legalRepDocuments_${fileIndex}_legalRepDocumentType"]:not([disabled])`
        );
        await typeDropdown.waitFor({ state: 'attached' });
        await expect(typeDropdown).toBeEnabled({ timeout: 60000 });

        await typeDropdown.selectOption({ label: document.type });

        await performActions(
          'Add Document',
          ['inputText', {text: uploadYourDocuments.shortDescriptionHiddenTextLabel, index: fileIndex}, document.description]
        );
      }
    }

    await performAction('clickButton', uploadYourDocuments.continueButton);
  }

}
