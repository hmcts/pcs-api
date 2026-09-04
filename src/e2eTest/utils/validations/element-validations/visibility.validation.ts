import {Page, expect, Locator} from '@playwright/test';
import {IValidation, validationData} from '@utils/interfaces';
import {MEDIUM_TIMEOUT} from '../../../playwright.config';

export class VisibilityValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data: validationData): Promise<void> {
    let element = page.locator(`label:text-is("${fieldName}"),
                                         span:text-is("${fieldName}")`);
    let selectors = fieldName == "" ? Array.isArray(data) ? data as string[] : [data as string] : [fieldName];
    let elements = selectors.map(selector => page.locator(`label:text-is("${selector}"),
                                         span:text-is("${selector}")`));

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

  /**
   * `all()` resolved immediately with whatever matched at that instant, so if the element
   * had not appeared yet it returned [] and the wait became a no-op — callers compensated
   * with fixed sleeps. `not.toBeVisible()` polls and, like the previous `state: 'hidden'`,
   * is satisfied by the element being hidden or absent.
   */
  private async waitUntilElementDisappears(element: Locator): Promise<void> {
    await expect(element.first()).not.toBeVisible({ timeout: MEDIUM_TIMEOUT });
  }
}
