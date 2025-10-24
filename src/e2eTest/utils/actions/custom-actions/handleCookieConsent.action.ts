import {Page, test, Locator} from '@playwright/test';
import {IAction, actionRecord} from '../../interfaces/action.interface';
import { SHORT_TIMEOUT, waitForLoadStateTimeout } from '../../../playwright.config';

export class handleCookieConsentAction implements IAction {
  async execute(page: Page, action: string, actionText: actionRecord): Promise<void> {
    const acceptButtonName = actionText.accept as string;
    const hideButtonName = actionText.hide as string | undefined;
    await test.step(`Handle cookie consent (if present): ${acceptButtonName}`, async () => {

      let consentBanner: Locator;
      let acceptBtn: Locator;
      let successBanner: Locator | undefined;
      let hideBtn: Locator | undefined;

      if (acceptButtonName === "Accept additional cookies") {
        consentBanner = page.locator('#cm_cookie_notification');
        acceptBtn = consentBanner.getByRole('button', {name: acceptButtonName});
        if (hideButtonName) {
          successBanner = page.locator('#accept-all-cookies-success');
          hideBtn = successBanner.getByRole('button', {name: hideButtonName});
        }
      } else if (acceptButtonName === "Accept analytics cookies") {
        consentBanner = page.locator('xuilib-cookie-banner');
        acceptBtn = consentBanner.getByRole('button', {name: acceptButtonName});
      }
        else {
        return;
      }
      try {
        await consentBanner.waitFor({ state: 'attached', timeout: 5000 });
        await acceptBtn.click({ timeout: 5000 });
        await consentBanner.waitFor({ state: 'hidden', timeout: SHORT_TIMEOUT });
        if (successBanner && hideBtn) {
          await successBanner.waitFor({ state: 'visible', timeout: SHORT_TIMEOUT });
          await hideBtn.click({ timeout: SHORT_TIMEOUT });
          await successBanner.waitFor({ state: 'hidden', timeout: SHORT_TIMEOUT });
        }
        await page.waitForLoadState('domcontentloaded', {timeout: waitForLoadStateTimeout});
      } catch (err) {
      }
    });
  }
}
