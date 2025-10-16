import { IAction } from '../../interfaces/action.interface';
import { Page, test } from '@playwright/test';

export class NavigateToUrlAndHandleCookies implements IAction {
  /**
   * Navigate to a URL and try to bypass the cookie banner by:
   *  1) setting a consent cookie for the target URL (preferred)
   *  2) navigating and waiting for networkidle
   *  3) falling back to clicking the cookie buttons if the banner still appears
   */
  async execute(page: Page, action: string, url: string): Promise<void> {
    await test.step(`Attempt to set consent cookie for ${url}`, async () => {
      try {
        // NOTE: using `url` in the cookie avoids needing to hardcode domain
        // Replace name/value with the real cookie for your app if needed
        const consentCookie = {
          name: 'cm_cookie_policy',
          value: '{"analytics":"accepted","functional":"accepted"}',
          url,             // safer than hardcoding a domain
          path: '/',
          httpOnly: false,
          secure: true,
          sameSite: 'Lax' as const,
        };

        await page.context().addCookies([consentCookie]);
        console.log(`Added consent cookie '${consentCookie.name}' for ${url}`);
      } catch (err) {
        console.warn('Failed to set cookie via context.addCookies():', err);
      }
    });

    await test.step(`Navigate to ${url}`, async () => {
      // waitUntil 'networkidle' is usually good for SPA apps so client code can run
      await page.goto(url, { waitUntil: 'networkidle' });
    });

    await test.step('Fallback: check cookie banner and click if visible', async () => {
      try {
        const cookieBanner = page.locator('div.govuk-cookie-banner');
        // check visibility quickly; don't throw if not found
        if (await cookieBanner.isVisible({ timeout: 3000 }).catch(() => false)) {
          console.log('Cookie banner visible — attempting UI fallback clicks.');

          // Try likely selectors; adjust to your app's selectors if different
          const acceptBtn = page.locator('#cookie-accept-submit, button:has-text("Accept all cookies")');
          const hideMsgBtn = page.locator('#cookie-accept-all-success-banner-hide, button:has-text("Hide message")');

          // click accept if visible and actionable
          if (await acceptBtn.count() && await acceptBtn.isVisible().catch(() => false)) {
            await acceptBtn.click().catch(e => console.warn('acceptBtn.click() failed:', e));
          }

          // sometimes a confirmation/hide button appears — try to click it
          if (await hideMsgBtn.count() && await hideMsgBtn.isVisible().catch(() => false)) {
            await hideMsgBtn.click().catch(e => console.warn('hideMsgBtn.click() failed:', e));
          }

          // ensure banner detached or at least not visible
          await cookieBanner.waitFor({ state: 'detached', timeout: 5000 }).catch(() => {
            console.warn('Cookie banner did not detach within timeout; continuing anyway.');
          });

          console.log('Cookie banner handling fallback completed.');
        } else {
          console.log('Cookie banner not visible — cookie bypass likely worked.');
        }
      } catch (err) {
        console.error('Error during cookie banner fallback handling:', err);
      }
    });
  }
}
