import { expect, Page } from "@playwright/test";
import { EnforcementCommonUtils } from "@utils/actions/element-actions/enforcementUtils.action";
import { IValidation, validationRecord } from "@utils/interfaces";

export class ValidatePrePopulatedValues implements IValidation {
  async validate(page: Page, validation: string, fieldName: validationRecord, data: validationRecord): Promise<void> {
    const validationsMap = new Map<string, () => Promise<void>>([
      ['validateRadioButtonValues', () => this.validateRadioButtonValues(page, fieldName as validationRecord, data as validationRecord)],
      ['validateInputTextValues', () => this.validateInputTextValues(page, fieldName as validationRecord, data as validationRecord)],
      ['validateCheckBoxSelection', () => this.validateCheckBoxSelection(page, fieldName as validationRecord, data as validationRecord)],
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

  private async validateInputTextValues(page: Page, fieldName: validationRecord, data: validationRecord): Promise<void> {
    let locator = page.locator(`//span[text()="${fieldName.textLabel}"]/parent::label/following-sibling::*[self::textarea or self::input][not(@disabled)]`);
    const count = await locator.count();
    if (count === 0) throw new Error(`Text field related to the label ${fieldName.textLabel} not found`);
    if (typeof fieldName !== 'string' && fieldName.index !== null) {
      locator = count > 1
        ? locator.nth(Number(fieldName.index))
        : locator.first();
    }
    let retrievedText = await locator.inputValue();
    expect(retrievedText, `The PrePopulated value for the text field: ${fieldName.textLabel as string} is ${data.expected as string} and the retrieved value is: ${retrievedText}`).toEqual(data.expected as string);

  }

  private async validateCheckBoxSelection(page: Page, fieldName: validationRecord, data: validationRecord): Promise<void> {
    const checkBoxes = page.locator(`//span[text()="${fieldName.question}"]/ancestor::fieldset[1]//child::input[@type='checkbox']`);
    let retrieved;
    let retrievedArray: string[] = [];
    let expectedString;
    const count = await checkBoxes.count();

    if (count === 0) throw new Error(`Radio button related to the question ${fieldName.question} not found`);

    expectedString = Array.isArray(data.expected)
      ? data.expected.map((ele) => EnforcementCommonUtils.formatPayLoadData(ele)).join(" ")
      : EnforcementCommonUtils.formatPayLoadData(data.expected as string);


    for (let i = 0; i < count; i++) {
      const checkBox = checkBoxes.nth(i);
      if (await checkBox.isChecked()) {
        const id = await checkBox.getAttribute("id") ?? "";
        const text = await page.locator(`label[for="${id}"]`).textContent() ?? "";
        retrievedArray.push(text);
      }
    }
    retrieved = retrievedArray.join(" ");
    expect(retrieved, `The PrePopulated value for the radio button: ${fieldName.question as string} is ${expectedString} and the retrieved value is: ${retrieved}`).toEqual(expectedString);

  }
}