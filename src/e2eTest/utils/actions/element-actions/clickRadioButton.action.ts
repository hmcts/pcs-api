import {Page} from '@playwright/test';

import {IAction} from '../../interfaces/action.interface';

export class ClickRadioButton implements IAction {
    async execute(page: Page, fieldName: string): Promise<void> {
        const locator = page
            .locator(`input[type="radio"] + label:has-text("${fieldName}")`);
        await locator.click();
    }
}
