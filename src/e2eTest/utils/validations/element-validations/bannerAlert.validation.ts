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
        // Either way there was no retry. `toHaveText` polls until the text matches or the budget
        // expires, which is the same wait expressed as an assertion instead of a sample.
        //
        // This is why caseTabs:96 kept flaking after #2648 fixed the stage before it: the failure
        // moved from `mainHeader 'Add a case note'` to this validation, which is the next
        // unretried read in that journey.
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
