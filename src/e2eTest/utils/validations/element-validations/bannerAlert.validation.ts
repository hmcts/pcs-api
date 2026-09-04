import {Page, expect, test} from '@playwright/test';
import {IValidation} from '../../interfaces/validation.interface';
import {LONG_TIMEOUT} from '../../../playwright.config';

export class BannerAlertValidation implements IValidation {
    async validate(page: Page, validation: string, data: string): Promise<void> {
        const locator = page.locator('div.alert-message').first();
        await locator.waitFor({ state: 'visible', timeout: LONG_TIMEOUT });
        const alertText = (await locator.textContent())?.trim();
        await test.step(`Found alert message: "${alertText}"`, async () => {
            const isPattern =
                data.includes('.*') ||
                data.startsWith('^') ||
                data.endsWith('$');
            if (isPattern) {
                const regex = new RegExp(data);
                expect(alertText, `Alert should match pattern: ${regex}`).toMatch(regex);
            } else {
                expect(alertText, `Alert should exactly match`).toBe(data);
            }
        });
    }
}
