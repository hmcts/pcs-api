import { expect, Page } from "@playwright/test";
import { IValidation, validationRecord } from "@utils/interfaces";

export class ValidatePrePopulatedValues implements IValidation {
  async validate(page: Page, validation: string, fieldName: validationRecord, data: validationRecord): Promise<void> {
    const validationsMap = new Map<string, () => Promise<void>>([
      ['validateRadioButtonValues', () => this.validateRadioButtonValues(page, fieldName as validationRecord, data as validationRecord)],
    ]);

    const validationToPerform = validationsMap.get(validation);
    if (!validationToPerform) throw new Error(`No action found for '${validation}'`);
    await validationToPerform();

  }
  private async validateRadioButtonValues(page: Page, fieldName: validationRecord, data: validationRecord): Promise<void> {

    const radioButtons = page.locator(`//span[text()="${fieldName.question}"]/ancestor::fieldset[1]//child::input[@type='radio']`);
    let retrieved;
    const count = await radioButtons.count();

    if (count === 0) throw new Error(`Radio button related to the question ${fieldName.question} not found`);
    for (let i = 0; i < count; i++) {
      const radioButton = radioButtons.nth(i);
      if (await radioButton.isChecked()) {
        const id = await radioButton.getAttribute("id") ?? "";
        retrieved = await page.locator(`label[for="${id}"]`).textContent() ?? "";
      }
    }
    expect(String(retrieved).toLowerCase(), `The PrePopulated value for the radio button: ${fieldName.question as string} is ${data.expected as string} and the retrieved value is: ${retrieved}`).toEqual(String(data.expected).toLowerCase());
  }
}