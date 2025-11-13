import { Page } from '@playwright/test';

/**
 * Handle pre-login cookie banner (on hmcts-access.service.gov.uk)
 */
export async function handlePreLoginCookieBanner(page: Page): Promise<void> {
  try {
    const banner = page.locator('#cm_cookie_notification');
    const acceptButton = page.locator('#cookie-accept-submit');

    const isVisible = await banner.waitFor({ state: 'attached', timeout: 5000 })
      .then(() => banner.isVisible())
      .catch(() => false);

    if (!isVisible) return;

    console.log('Pre-login cookie banner detected, accepting cookies...');
    await acceptButton.click({ timeout: 5000 });
    await banner.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {});

    // Handle success banner if present
    const successBanner = page.locator('#accept-all-cookies-success');
    if (await successBanner.isVisible({ timeout: 2000 }).catch(() => false)) {
      await successBanner.getByRole('button', { name: 'Hide this cookie message' })
        .click({ timeout: 2000 })
        .catch(() => {});
      await successBanner.waitFor({ state: 'hidden', timeout: 2000 }).catch(() => {});
    }

    console.log('✓ Pre-login cookies accepted');
  } catch (error) {
    console.warn('Pre-login cookie banner not found or failed:', (error as Error).message);
  }
}

/**
 * Handle post-login cookie banner (on the service)
 */
export async function handlePostLoginCookieBanner(page: Page): Promise<void> {
  try {
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});

    // Try multiple selectors for the cookie banner
    const selectors = ['xuilib-cookie-banner', 'xuilib-cookie-banner .govuk-cookie-banner', '[class*="cookie-banner"]', 'govuk-cookie-banner'];

    let banner = null;
    let acceptButton = null;

    for (const selector of selectors) {
      try {
        const locator = page.locator(selector).first();
        await locator.waitFor({ state: 'attached', timeout: 3000 }).catch(() => {});

        if (await locator.isVisible({ timeout: 2000 }).catch(() => false)) {
          banner = locator;
          acceptButton = locator.getByRole('button', { name: /Accept analytics cookies/i });

          if (!(await acceptButton.isVisible({ timeout: 1000 }).catch(() => false))) {
            acceptButton = locator.locator('button:has-text("Accept analytics cookies")');
          }

          if (await acceptButton.isVisible({ timeout: 1000 }).catch(() => false)) {
            console.log(`Post-login cookie banner detected (selector: ${selector}), accepting analytics cookies...`);
            break;
          }
        }
      } catch {
      }
    }

    if (!banner || !acceptButton || !(await banner.isVisible({ timeout: 2000 }).catch(() => false))) {
      console.log('Post-login cookie banner not found or already dismissed');
      return;
    }

    // Wait for spinners and ensure page is stable
    await page.locator('.spinner-container, .loading, [class*="spinner"]')
      .waitFor({ state: 'detached', timeout: 5000 })
      .catch(() => {});
    await page.waitForTimeout(500);
    await acceptButton.scrollIntoViewIfNeeded({ timeout: 5000 }).catch(() => {});

    // Click with fallback to force click
    await acceptButton.click({ timeout: 5000 }).catch(async () => {
      console.log('Normal click failed, trying force click...');
      await acceptButton.click({ force: true, timeout: 5000 });
    });

    // Wait for banner to disappear
    await banner.waitFor({ state: 'hidden', timeout: 5000 }).catch(() => {
      return banner.waitFor({ state: 'detached', timeout: 5000 }).catch(() => {});
    });

    console.log('✓ Post-login analytics cookies accepted');
  } catch (error) {
    console.warn('Post-login cookie banner not found or failed:', (error as Error).message);
  }
}

