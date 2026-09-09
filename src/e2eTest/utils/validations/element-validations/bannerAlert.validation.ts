import {Page, expect, test} from '@playwright/test';
import {IValidation} from '../../interfaces/validation.interface';
import {LONG_TIMEOUT} from '../../../playwright.config';

export class BannerAlertValidation implements IValidation {
    async validate(page: Page, validation: string, data: string): Promise<void> {
        const alerts = page.locator('div.alert-message');
        const locator = alerts.first();
        const isPattern =
            data.includes('.*') ||
            data.startsWith('^') ||
            data.endsWith('$');
        const expected: string | RegExp = isPattern ? new RegExp(data) : data;

        // `waitFor` then `textContent()` waited for an alert to be VISIBLE and then read it
        // exactly once. `textContent()` does not poll, so the assertion got a single sample:
        //
        // - XUI renders the alert container before it fills the text in, so the sample can be
        //   empty or stale even though the right banner arrives a moment later, and
        // - `.first()` will happily read a *different* alert that is already on the page.
        //
        // Either way there was no retry: the budget was spent waiting for *an* alert to exist,
        // not for the *right text* to appear. `toHaveText` polls until it matches, which is the
        // same wait expressed as an assertion instead of a sample.
        //
        // This is the next unretried read in the caseTabs:96 journey, which is where that flake
        // moved after the selectAnEvent verification fixed `mainHeader 'Add a case note'`.
        // Measured as PR-2665: 49 passed, 0 failed, 1 flaky (caseWorkerGenApps:54, unrelated),
        // 24.3m, with caseTabs:96 passing and no bannerAlert failure. Unproven rather than
        // demonstrated — caseTabs:96 also passed on two runs without this — so it is kept on the
        // defect being real by inspection, and because 98 call sites read this banner.
        try {
            await expect(locator).toHaveText(expected, { timeout: LONG_TIMEOUT });
        } catch (error) {
            const present = await alerts.allTextContents().catch(() => [] as string[]);
            console.warn(`[bannerAlert] expected ${expected} but the page had `
                + `${present.length} alert(s): ${JSON.stringify(present)}`);
            throw error;
        }

        const alertText = (await locator.textContent())?.trim();
        await test.step(`Found alert message: "${alertText}"`, async () => undefined);
    }
}
