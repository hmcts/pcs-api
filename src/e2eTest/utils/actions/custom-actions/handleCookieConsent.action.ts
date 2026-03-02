import {Page, test, Locator} from '@playwright/test';
import {IAction, actionRecord} from '@utils/interfaces';
import {MEDIUM_TIMEOUT, VERY_SHORT_TIMEOUT} from '../../../playwright.config';

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
      } else {
        return;
      }

      const isBannerVisible = await consentBanner.isVisible().catch(() => false);

      if (!isBannerVisible) {
        return;
      }

      try {
        await acceptBtn.waitFor({ state: 'visible', timeout: MEDIUM_TIMEOUT });
        await page.waitForTimeout(VERY_SHORT_TIMEOUT);
        await acceptBtn.click();

        await consentBanner.waitFor({ state: 'hidden', timeout: MEDIUM_TIMEOUT }).catch(() => {
          return consentBanner.waitFor({ state: 'detached', timeout: MEDIUM_TIMEOUT });
        });

        if (successBanner && hideBtn) {
          const isSuccessBannerVisible = await successBanner.isVisible().catch(() => false);

          if (isSuccessBannerVisible) {
            await hideBtn.waitFor({ state: 'visible', timeout: MEDIUM_TIMEOUT });
            await page.waitForTimeout(VERY_SHORT_TIMEOUT);
            await hideBtn.click();
            await successBanner.waitFor({ state: 'hidden', timeout: MEDIUM_TIMEOUT }).catch(() => {
              return successBanner.waitFor({ state: 'detached', timeout: MEDIUM_TIMEOUT });
            });
          }
        }
      } catch (error) {
        const errorMessage = (error as Error).message;
        if (!errorMessage.includes('timeout') && !errorMessage.includes('waiting for')) {
          console.log(`Cookie consent handling encountered an issue: ${acceptButtonName} - ${errorMessage}`);
        }
      }
    });
  }
}
