import {Page, test, Locator} from '@playwright/test';
import {IAction, actionRecord} from '@utils/interfaces';
import {MEDIUM_TIMEOUT, SHORT_TIMEOUT} from '../../../playwright.config';
import {performAction} from '@utils/controller';
import {signInOrCreateAnAccount} from '@data/page-data';

export class handleCookieConsentAction implements IAction {
  async execute(page: Page, action: string, actionText: actionRecord): Promise<void> {
    const acceptButtonName = actionText.accept as string;
    const hideButtonName = actionText.hide as string | undefined;
    await test.step(`Handle cookie consent (if present): ${acceptButtonName}`, async () => {
      let consentBanner: Locator;
      let successBanner: Locator | undefined;

      if (acceptButtonName === signInOrCreateAnAccount.acceptAdditionalCookiesButton) {
        consentBanner = page.locator('#cm_cookie_notification');
        if (hideButtonName) {
          successBanner = page.locator('#accept-all-cookies-success');
        }
      } else if (acceptButtonName === signInOrCreateAnAccount.acceptAnalyticsCookiesButton) {
        consentBanner = page.locator('xuilib-cookie-banner');
      } else {
        return;
      }

      try {
        await consentBanner.waitFor({ state: 'visible', timeout: MEDIUM_TIMEOUT });
        await performAction('clickButton', acceptButtonName);
        await consentBanner.waitFor({ state: 'hidden', timeout: MEDIUM_TIMEOUT });

        if (successBanner && hideButtonName) {
          await successBanner.waitFor({ state: 'visible', timeout: MEDIUM_TIMEOUT });
          const hideButton = page.locator('#cookie-accept-all-success-banner-hide');
          await hideButton.waitFor({ state: 'visible', timeout: MEDIUM_TIMEOUT });
          await hideButton.scrollIntoViewIfNeeded({ timeout: SHORT_TIMEOUT });
          await hideButton.click({ timeout: MEDIUM_TIMEOUT });
          await successBanner.waitFor({ state: 'hidden', timeout: MEDIUM_TIMEOUT });
        }

        await page.waitForLoadState('domcontentloaded', { timeout: MEDIUM_TIMEOUT });
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : String(err);
        if (errorMessage.includes('waiting for') || errorMessage.includes('timeout')) {
          console.warn(`Cookie consent banner not found or timeout: ${acceptButtonName}. This is usually safe to ignore if cookies were already accepted.`, errorMessage);
        } else {
          console.warn(`Cookie consent action failed: ${acceptButtonName}`, errorMessage);
        }
      }
    });
  }
}
