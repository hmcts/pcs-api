import { Page } from '@playwright/test';
import { actionRecord, IAction } from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldParams: string | actionRecord, value: string): Promise<void> {


    let locator;
    if (typeof fieldParams !== 'string' && fieldParams.index !== null) {
      const labelText = fieldParams.textLabel ?? fieldParams.text;
      locator = page.locator(`//span[text()="${labelText}"]/parent::label/following-sibling::*[self::textarea or self::input][not(@disabled)]`);

      locator = (await locator.count()) > 1
        ? locator.nth(Number(fieldParams.index))
        : locator.first();
    } else {
      locator = typeof fieldParams === 'string'
        ? await this.getStringFieldLocator(page, fieldParams)
        : page.locator(`fieldset:has(h2:has-text("${fieldParams.text}")) textarea:visible:enabled,
      :has-text("${fieldParams.text}") ~ input:visible:enabled,
      label:has-text("${fieldParams.text}") ~ textarea,
      :has-text("${fieldParams.text}") ~ textarea:visible:enabled`).first();
    }
    await locator.fill(value);
  }

  private async getStringFieldLocator(page: Page, fieldParams: string) {
    const roleLocator = page.getByRole('textbox', { name: fieldParams, exact: true });
    // .first() because the object branch in execute() has it and this one did not: a repeated
    // CCD collection gives several textboxes the same accessible name, and fill() is strict.
    // That failure is fast (18ms) rather than the 40s timeout below, but it is the same
    // one-field-two-matches situation the rest of this class already guards against.
    return (await roleLocator.count() > 0)
      ? roleLocator.first()
      // `+ div input` was the only branch here without `:visible:enabled`, so it could
      // resolve to a hidden or disabled input — CCD renders those routinely for
      // conditionally shown fields. fill() then waits for an element that will never become
      // editable and burns its entire 40s actionTimeout, reported as
      // "locator.fill: Timeout 40000ms exceeded" as seen on createCase.spec.ts:1043.
      // Reproduced: a hidden input reached this way times out in full (4003ms at a 4s
      // timeout), whereas a strict-mode violation fails in 18ms — so the timeout signature
      // points at this, not at an ambiguous selector.
      : page.locator(`:has-text("${fieldParams}") ~ input:visible:enabled,
                      label:has-text("${fieldParams}") ~ textarea:visible:enabled,
                      label:has-text("${fieldParams}") + div input:visible:enabled`).first();
  }
}
