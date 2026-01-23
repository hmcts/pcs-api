import {Page, expect, Locator} from '@playwright/test';
import {IValidation, validationData} from '@utils/interfaces';

export class VisibilityValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data: validationData): Promise<void> {
    let element = page.locator(`label:has-text("${fieldName}"),
                                         span:has-text("${fieldName}"),
                                         button:has-text("${fieldName}")`);
    let selectors = fieldName == "" ? Array.isArray(data) ? data as string[] : [data as string] : [fieldName];
    let elements = selectors.map(selector => page.locator(`label:has-text("${selector}"),
                                         span:has-text("${selector}")`));

    const validationsMap = new Map<string, () => Promise<void>>([
      ['elementToBeVisible', () => this.elementToBeVisible(element)],
      ['elementNotToBeVisible', () => this.elementNotToBeVisible(elements)],
      ['waitUntilElementDisappears', () => this.waitUntilElementDisappears(element)]
    ]);
    const validationToPerform = validationsMap.get(validation);
    if (!validationToPerform) throw new Error(`No action found for '${validation}'`);
    await validationToPerform();
  }

  private async elementToBeVisible(element: Locator): Promise<void> {
    await expect(element).toBeVisible();
  }

  private async elementNotToBeVisible(elements: Locator[]): Promise<void> {
    for (const element of elements) {
      await expect(element).not.toBeVisible();
    }
  }

  private async waitUntilElementDisappears(element: Locator): Promise<void> {
    const elements = await element.all();
    await Promise.all(elements.map(element => element.waitFor({ state: 'hidden', timeout: 10000 })));
  }
}
