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
