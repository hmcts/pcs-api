import test, {expect, Locator, Page} from '@playwright/test';
import { getFormattedDate } from "@utils/common/string.utils";

import {
  checkYourAnswersUploadAdditionalDocs,
  confirmIfTheseDocumentsRelateToAnApplication, documentsUploadConfirm,
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
import {compareMaps} from "@utils/common/compareMaps.util";
import {FieldsStore} from "@utils/actions/custom-actions";

const cyaMap = new Map<string, string>();
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
      ['retrieveCYATableDataLR', () => this.retrieveCYATableDataLR(page, fieldName as actionRecord)],
      ['validateCYAForLR', () => this.validateCYAForLR(page)],
      ['readDocumentsSubmit', () => this.readDocumentsSubmit()],
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
    // Record the answer so FieldsStore matches what CYA will show
    FieldsStore.set(
      confirmIfTheseDocumentsRelateToAnApplication.doTheseDocumentsQuestion,
      selectOption.replace(/\s+/g, ' ').trim()
    );
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

        // Exact id, not a `^=` prefix. The prefix also matches the ...defendantDocumentTypeWales
        // sibling, so on a page rendering both this resolves to 2 elements and selectOption -
        // being strict - throws immediately. Measured: prefix count=2 and a strict-mode
        // violation in 7ms; exact count=1.
        const typeDropdown = page.locator(
          `[id="lrDocUpload_LegalRepDocuments_${fileIndex}_defendantDocumentType"]:not([disabled])`
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

// class-level fields, alongside the existing cyaMap
  private cyaChangeLinksMap: Map<string, { text: string; href: string; locator: Locator } | null> = new Map();

  private async retrieveCYATableDataLR(page: Page, table: actionRecord) {
    const tables = page.locator(`//table[@aria-describedby="${table.name}"]`);
    const tableCount = await tables.count();

    if (tableCount === 0 && table.name === 'check your answers table') throw new Error(`the table ${table.name} not found. Exiting...`);

    for (let i = 0; i < tableCount; i++) {
      const table = tables.nth(i);
      await expect(table).toBeVisible();

      // only direct child rows of this table - excludes rows belonging to any
      // nested complex-panel-table (e.g. "Add document 1" -> "Type of document"),
      // which are answers *within* a row's content cell, not top-level CYA rows.
      const rows = table.locator('> tbody > tr, > tr');
      const rowCount = await rows.count();

      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        if (!(await row.isVisible())) continue;

        const keyQns = row.locator('th span, th');
        const valAns = row.locator('td.case-field-content, td');
        const changeCell = row.locator('td.case-field-change, td.check-your-answers__change');

        if ((await keyQns.count()) === 0 || (await valAns.count()) === 0) continue;

        const keyText = (await keyQns.first().innerText()).trim();
        const valText = (await valAns.first().innerText()).trim().replace(/\r?\n+/g, ',');
        if (keyText && keyText.length > 0) {
          cyaMap.set(keyText ?? '', valText ?? '');
        }

        // capture the change link for this row, if a change cell exists
        if ((await changeCell.count()) > 0) {
          const changeLink = changeCell.first().locator('a');
          if ((await changeLink.count()) > 0) {
            const href = (await changeLink.first().getAttribute('href')) ?? '';
            const linkText = (await changeLink.first().innerText()).trim();
            this.cyaChangeLinksMap.set(keyText, { text: linkText, href, locator: changeLink.first() });
          } else {
            this.cyaChangeLinksMap.set(keyText, null); // row has a change cell but no <a> inside it
          }
        }
      }
    }
    cyaMap.delete('Add document');
    this.cyaChangeLinksMap.delete('Add document');

    await test.step('Retrieved CYA values can be found in the console logs', async () => {
      console.log('\nThe Data Retrieved From Check Your Answers Page Are As Follows');
      const lines: string[] = [];
      for (const [key, value] of cyaMap.entries()) {
        const line = `• Key: "${key}" → Value: "${value}"`;
        console.log('============================================================');
        console.log(line);
        lines.push(line);
      }
    });
    await page.waitForLoadState('networkidle');
  }

  private async validateCYAForLR(page: Page) {
    const misMatchMap = compareMaps(cyaMap, FieldsStore.getAll(), {
      name1: 'CYA',
      name2: 'FieldStore',
    });

    await test.step('CYA Validation Started and the results are present in the console logs', async () => {
      if (misMatchMap.size > 0) {
        console.log(`\n❌ Differences found: ${misMatchMap.size}`);
        for (const [key, val] of misMatchMap) {
          const expectedValue = val.a === undefined ? '<missing>' : String(val.a);
          const actualValue = val.b === undefined ? '<missing>' : String(val.b);
          console.log('============================================================');
          console.log(`• key: "${String(key)}" → Expected: ${expectedValue} | Actual: ${actualValue}`);
        }
        console.log(`\n**********  END OF CYA FAILURE LIST. ***************`);
        throw new Error(`CYA validations failed for ${misMatchMap.size} ${misMatchMap.size === 1 ? 'item' : 'items'}`);
      } else {
        console.log('\n✅ CHECK YOUR ANSWERS VALIDATION PASSED!\n');
      }
    });

    cyaMap.clear();

    // click each row's Change link, confirm it lands on the page where that
    // question was originally answered, then return to the CYA table.
    await this.validateChangeLinks(page);

    // ensure the CYA page/table is fully settled before the caller clicks Submit -
    // stops a race where Submit is clicked while Angular is still finishing render
    await test.step('Waiting for CYA page to be fully settled before Submit', async () => {
      const cyaTable = page.locator('//table[@aria-describedby="check your answers table"]');
      await expect(cyaTable.first()).toBeVisible({ timeout: 15000 });
      await page.waitForLoadState('networkidle');
    });
    await performAction(
      'clickButton',
      checkYourAnswersUploadAdditionalDocs.submitButton
    );
  }
  /**
   * For every row captured in cyaChangeLinksMap: clicks its Change link,
   * confirms the resulting page actually shows the field for that question
   * (not just "a" page), then navigates back to the check-your-answers table
   * before moving on to the next row.
   */
  private async validateChangeLinks(page: Page) {
    const misMatches: string[] = [];
    const cyaTable = page.locator('//table[@aria-describedby="check your answers table"]');

    await test.step('Validating change links navigate to the correct question page', async () => {
      for (const [key, changeInfo] of this.cyaChangeLinksMap.entries()) {
        if (!changeInfo) {
          misMatches.push(`• Key: "${key}" → expected a Change link but none was found`);
          continue;
        }

        const { locator } = changeInfo;

        if (!(await locator.isVisible()) || !(await locator.isEnabled())) {
          misMatches.push(`• Key: "${key}" → Change link not usable (visible/enabled check failed)`);
          continue;
        }

        await locator.click();
        await page.waitForLoadState('networkidle');

        const landedOnCorrectPage = await this.isQuestionFieldVisible(page, key);
        if (!landedOnCorrectPage) {
          misMatches.push(`• Key: "${key}" → Change link did not land on the page where this question is answered`);
        }

        // return to the CYA table before checking the next row
        await page.goBack();
        await expect(cyaTable).toBeVisible();
      }
    });

    if (misMatches.length > 0) {
      console.log(`\n❌ Change link navigation failures: ${misMatches.length}`);
      misMatches.forEach(line => {
        console.log('============================================================');
        console.log(line);
      });
      console.log(`\n**********  END OF CHANGE LINK FAILURE LIST. ***************`);
      throw new Error(`Change link navigation failed for ${misMatches.length} ${misMatches.length === 1 ? 'item' : 'items'}`);
    } else {
      console.log('\n✅ CHANGE LINK NAVIGATION VALIDATION PASSED!\n');
    }

    this.cyaChangeLinksMap.clear();
  }

  /**
   * Checks whether the given CYA question text appears as the field label
   * for the current page - i.e. this is genuinely the page where that
   * question was answered, not just some other page in the journey.
   * Checks legend (radio/checkbox groups), label (text/select inputs), and
   * headings, since CCD renders different field types with different tags.
   */
  private async isQuestionFieldVisible(page: Page, questionText: string): Promise<boolean> {
    const candidates = [
      page.locator('legend', { hasText: questionText }),
      page.locator('label', { hasText: questionText }),
      page.locator('h1', { hasText: questionText }),
      page.locator('h2', { hasText: questionText }),
    ];

    for (const candidate of candidates) {
      if ((await candidate.count()) > 0 && (await candidate.first().isVisible())) {
        return true;
      }
    }
    return false;
  }

  private async readDocumentsSubmit(): Promise<void> {
    await performValidation('elementToBeVisible', documentsUploadConfirm.documentsParagraph);
  }
}
