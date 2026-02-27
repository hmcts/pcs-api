import { Page, expect, Locator } from '@playwright/test';
import { IValidation, validationData } from '@utils/interfaces';

export class FormLabelValueValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data?: validationData): Promise<void> {
    const valueLocator = await this.findFieldValueLocator(page, fieldName);

    if (data !== undefined) {
      const locText = await valueLocator.innerText();
      expect(locText.replace(/\s+/g, ' ').trim().split(' ').sort().join(' '),`Original inner text => ${locText}, Original user input => ${String(data)}`).toBe(String(data).split(' ').sort().join(' '));
    } else {
      const value = await valueLocator.textContent();
      if (!value?.trim()) {
        throw new Error(`Value for "${fieldName}" is empty`);
      }
    }
  }

  private async findFieldValueLocator(page: Page, fieldName: string): Promise<Locator> {
    const locators = [
      page.locator(`.case-viewer-label:text-is("${fieldName}")`)
        .locator('xpath=../following-sibling::td[1]')
        .locator('.text-16 span'),

      page.locator(`th#complex-panel-simple-field-label > span.text-16:text-is("${fieldName}")`)
        .locator('xpath=../..')
        .locator('td span.text-16:not(:has(ccd-field-read-label))'),

      page.locator(`//th[normalize-space(.)="${fieldName}"]/following-sibling::td`),

      page.locator(`//p[normalize-space(.)="${fieldName}"]/following-sibling::ul[1]/li[1]`)
    ];

    for (const locator of locators) {
      if (await locator.count() === 1  && await locator.isVisible()) {
        return locator;
      }
    }
    throw new Error(`Field "${fieldName}" not found`);
  }
}
