import { test as setup } from '@playwright/test';
import { initializeExecutor, performAction } from '@utils/controller';
import { user } from '@data/user-data';
import CookiesHelper from '@utils/helpers/cookies-helper';
import PageCookiesManager from '@utils/helpers/page-cookies-manager';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';

const USER_KEY = 'permanent-user';
const runSetup = process.env.PLAYWRIGHT_RUN_SETUP === 'true';

setup('Authenticate user and save cookies', async ({ page }) => {
  if (!runSetup) {
    console.log('Skipping authenticate user and save cookies setup');
    console.log('User will be logged in via IDAM when needed during each test execution');
    return;
  }
  
  console.log('Setting up authentication cookies...');
  
  initializeExecutor(page);
  
  // Navigate to login page
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAdditionalCookiesButton,
    hide: signInOrCreateAnAccount.hideThisCookieMessageButton
  });
  
  // Perform login
  await performAction('login', user.claimantSolicitor);
  
  // Wait for login to complete - navigate to a page that requires authentication
  await page.waitForURL(/.*\/cases.*/, { timeout: 30000 }).catch(async () => {
    // If URL doesn't match, wait a bit more
    await page.waitForTimeout(3000);
  });
  
  // Get and save cookies
  const cookiesManager = new PageCookiesManager(page);
  const cookies = await cookiesManager.getCookies();
  await CookiesHelper.writeCookies(cookies, USER_KEY);
  
  console.log(`Authentication setup complete. Cookies saved for user: ${USER_KEY}`);
});

