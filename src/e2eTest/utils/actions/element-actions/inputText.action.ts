import { Page } from '@playwright/test';
import { actionRecord, IAction } from '../../interfaces/action.interface';
// Short on purpose: most callers pass an index the page can never satisfy. See execute().
const INDEXED_FIELD_TIMEOUT = 3000;

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldParams: string | actionRecord, value: string): Promise<void> {


    let locator;
    if (typeof fieldParams !== 'string' && fieldParams.index !== null) {
      const labelText = fieldParams.textLabel ?? fieldParams.text;
      locator = page.locator(`//span[text()="${labelText}"]/parent::label/following-sibling::*[self::textarea or self::input][not(@disabled)]`);

      // Wait for the indexed field before reading a non-polling count(): if it has not rendered,
      // count() returns 1 and the value overwrites the FIRST party's field. Only paid when the
      // field is missing, and capped, because callers pass an index whenever a question *can*
      // repeat — on single-field pages nth(index) never attaches.
      const index = Number(fieldParams.index);
      const hasIndex = Number.isInteger(index) && index > 0;
      if (hasIndex && (await locator.count()) <= index) {
        const waitStarted = Date.now();
        await locator.nth(index).waitFor({ state: 'attached', timeout: INDEXED_FIELD_TIMEOUT })
          .catch(() => undefined);
        if ((await locator.count()) <= index) {
          console.warn(`[inputText] index=${index} never appeared for "${labelText}" after `
            + `${Date.now() - waitStarted}ms; filling the first matching field instead`);
        }
      }
      locator = hasIndex && (await locator.count()) > 1
        ? locator.nth(index)
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
    // .first(): a repeated CCD collection gives several textboxes the same accessible name and
    // fill() is strict.
    return (await roleLocator.count() > 0)
      ? roleLocator.first()
      // `:visible:enabled` on every branch: one could resolve to a hidden input, and fill() then
      // burns its whole 40s actionTimeout (createCase.spec.ts:1043).
      : page.locator(`:has-text("${fieldParams}") ~ input:visible:enabled,
                      label:has-text("${fieldParams}") ~ textarea:visible:enabled,
                      label:has-text("${fieldParams}") + div input:visible:enabled`).first();
  }
}
