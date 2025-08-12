import {Page, Locator} from '@playwright/test';

import {IAction} from '../../interfaces/action.interface';

export class ClickRadioButton implements IAction {
    locator?: Locator;
    async execute(page: Page, action:string, fieldName: string, value: string): Promise<void> {
        let locator = page
            .locator(`input[type="radio"] + label:has-text("${fieldName}")`);
        if (await locator.count() > 1) {
            locator = page.locator(
                `:has-text("${value}") ~ div > label:has-text("${fieldName}")`
            );
        }
        await locator.click();
    }
}
