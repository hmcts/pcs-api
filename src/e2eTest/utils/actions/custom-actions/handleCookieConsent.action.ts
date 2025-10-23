import {actionRecord, IAction} from '../../interfaces/action.interface';
import {Page} from '@playwright/test';
import {customTimeout} from "../../../playwright.config";

export class handleCookieConsentAction implements IAction {
  async execute(page: Page, action: string, actionText: actionRecord): Promise<void> {
    const button = page.getByRole('button', { name: `${actionText.accept}` });
    try {
      await button.waitFor({ state: 'visible', timeout: customTimeout });
      await button.click();
      if(actionText.hide) {
        await page.getByRole('button', { name: `${actionText.hide}` }).click();
      }
    } catch {
      console.log(`Cookie consent button not found or not visible: ${actionText.accept}`);
    }
  }
}
