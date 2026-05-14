import { test } from '@utils/test-fixtures';
import {
  initializeExecutor,
  performAction,
} from '@utils/controller';
import {
  home
} from '@data/page-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import {staff} from "@data/user-data/staff.user.data";

test.use({ storageState: undefined });

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await page.evaluate(() => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch (e) {
      // Ignore if storage is not accessible
    }
  });

  await dismissCookieBanner(page, 'additional');
  await performAction('login', staff.CTSCAdmin.email);
  await dismissCookieBanner(page, 'analytics');
});

test.describe('[Create Case - With resume claim options] @CCPR', async () => {
  test('England - Resume with saved options - Assured Tenancy - Rent arrears + other grounds when user selects no to rent arrears question', async () => {
    await performAction('clickTab', home.createCaseTab);
  });
});

