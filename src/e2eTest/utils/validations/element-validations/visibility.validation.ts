import {Page, expect, Locator} from '@playwright/test';
import {IValidation, validationData} from '../../interfaces/validation.interface';

export class VisibilityValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data: validationData): Promise<void> {
    let element = page.locator(`label:has-text("${fieldName}")`);
    const validationsMap = new Map<string, () => Promise<void>>([
      ['elementToBeVisible', () => this.elementToBeVisible(element)],
      ['elementNotToBeVisible', () => this.elementNotToBeVisible(element)],
      ['waitUntilElementDisappears', () => this.waitUntilElementDisappears(element)]
    ]);
    const validationToPerform = validationsMap.get(validation);
    if (!validationToPerform) throw new Error(`No action found for '${validation}'`);
    await validationToPerform();
  }

  private async elementToBeVisible(element: Locator): Promise<void> {
    await expect(element).toBeVisible();
  }

  private async elementNotToBeVisible(element: Locator): Promise<void> {
    await expect(element).not.toBeVisible();
  }

  private async waitUntilElementDisappears(element: Locator): Promise<void> {
    await element.waitFor({ state: 'hidden', timeout: 10000 });
  }
}
