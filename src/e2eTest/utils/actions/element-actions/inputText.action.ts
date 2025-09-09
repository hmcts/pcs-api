import { Page } from '@playwright/test';
import {actionRecord, IAction} from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldParams: string | actionRecord, value: string): Promise<void> {
    const locator = typeof fieldParams === 'string'
      ? page.locator(`:has-text("${fieldParams}") ~ input,
           label:has-text("${fieldParams}") ~ textarea,
           label:has-text("${fieldParams}") + div input`)
      : page.locator(`fieldset:has(h2:text-is("${fieldParams.text}")) textarea:visible:enabled`).nth(Number(fieldParams.index));
    try{
      await locator.fill(value);
    }
    catch(error){
    await locator.first().fill(value);
    }
  }
}
