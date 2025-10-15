import { Page, test} from '@playwright/test';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {provideMoreDetailsOfClaim} from '@data/page-data/provideMoreDetailsOfClaim.page.data';
import {user} from '@data/user-data/permanent.user.data';
import {home} from '@data/page-data/home.page.data';

export class CommonActions {
  /**
   * A robust, hybrid function to handle the cookie banner. It combines the cookie injection
   * strategy with a UI fallback for maximum reliability.
   *
   * HOW TO USE:
   * Instead of calling `page.goto(url)`, you will now call:
   * `await CommonActions.navigateAndHandleCookieBanner(page, url);`
   *
   * HOW TO FIND THE CORRECT COOKIE VALUES (CRITICAL STEP):
   * 1. Open your website in a fresh incognito browser window.
   * 2. Open the browser's Developer Tools (usually F12).
   * 3. Go to the "Application" tab (in Chrome/Edge) or "Storage" tab (in Firefox).
   * 4. Under "Storage", clear any existing cookies for the site.
   * 5. Manually click the "Accept additional cookies" button on your website's banner.
   * 6. In the Developer Tools, go to "Cookies". A new cookie that manages consent will have appeared.
   * 7. Carefully copy its exact `name`, `value`, and `domain` and use them to update the `consentCookie` object below. The values here are placeholders.
   *
   * @param page The Playwright Page object.
   * @param url The URL to navigate to.
   */
  static async navigateAndHandleCookieBanner(page: Page, url: string): Promise<void> {
    // STRATEGY 1: Attempt to bypass by setting the cookie before navigation.
    await test.step('Attempt to bypass cookie banner by setting cookie', async () => {
      // IMPORTANT: You MUST replace the values below with the actual values from your application
      // by following the instructions detailed above.
      const consentCookie = {
        name: 'cm_cookie_policy', // Replace with your actual cookie name
        value: '{"analytics":"accepted","functional":"accepted"}', // Replace with your actual cookie value
        domain: '.hmcts-access.service.gov.uk', // Replace with your actual domain
        path: '/',
        httpOnly: false,
        secure: true,
        sameSite: 'Lax' as const,
      };

      await page.context().addCookies([consentCookie]);
      console.log(`Attempted to set consent cookie: '${consentCookie.name}'`);
    });

    // Navigate to the target page using a more robust wait condition.
    await test.step(`Maps to URL: ${url}`, async () => {
      // Using 'networkidle' waits for the network to be quiet, which is more reliable for
      // pages with client-side rendering that might add the banner after the initial DOM is loaded.
      await page.goto(url, { waitUntil: 'networkidle' });
    });

    // STRATEGY 2 (FALLBACK): Check if the banner is still visible and handle via UI if it is.
    await test.step('Fallback: Check for banner and handle with UI click if present', async () => {
      const cookieBannerContainer = page.locator('div.govuk-cookie-banner');

      // We check if the banner is visible using a short timeout.
      // The isVisible() check is better here than waitFor() because it doesn't throw an error if not found.
      if (await cookieBannerContainer.isVisible({ timeout: 3000 })) {
        console.log('Cookie bypass failed or was not effective. Banner is visible. Falling back to UI interaction.');

        try {
          const acceptCookiesButton = page.locator('#cookie-accept-submit');
          const hideMessageButton = page.locator('#cookie-accept-all-success-banner-hide');

          await acceptCookiesButton.click();
          await hideMessageButton.waitFor({ state: 'visible', timeout: 5000 });
          await hideMessageButton.click();
          console.log('Successfully handled cookie banner via UI fallback.');
        } catch (uiError) {
          console.error('An error occurred during the UI fallback for cookie banner handling:', uiError);
        }

      } else {
        // This is the ideal outcome, meaning the cookie injection worked and the banner never appeared.
        console.log('Cookie bypass successful. Banner was not found.');
      }
    });
  }
}




test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  // @ts-ignore
  await CommonActions.navigateAndHandleCookieBanner(page, process.env.MANAGE_CASE_BASE_URL);
  await performAction('login', user.claimantSolicitor);
  await page.waitForTimeout(2000);
  await performAction('clickTab', home.createCaseTab);
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.describe('[Create Case - England] @Master @nightly', async () => {
  test('England - Assured tenancy with Rent arrears and other possession grounds', async ({page}) => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
    await performValidation('compareWithFigma','claimantType');
    await performAction('selectClaimantType', claimantType.registeredProviderForSocialHousing);
  });
});
