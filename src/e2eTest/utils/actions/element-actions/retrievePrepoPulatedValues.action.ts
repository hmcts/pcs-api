import { Page } from "@playwright/test";

export class RetrievePrePopulatedValues {
  public static async retrievePrePopulatedValues<T>(page: Page, element: string, questionLabel: string, inputLabel: string): Promise<T> {

    switch (element) {
      case 'radioButton':
        const value = this.retrieveRadioButtonValues(page, questionLabel);
        return value as T

      default:
        throw new Error(`Unsupported element type: ${element}`);
    }

  }
  static async retrieveRadioButtonValues(page: Page, questionLabel: string): Promise<string> {
    const radios = page.locator(`//span[text()="${questionLabel}"]/ancestor::fieldset[1]//child::label[text()=""]/preceding-sibling::input[@type='radio']`);

    const count = await radios.count();

    for (let i = 0; i < count; i++) {
      const radio = radios.nth(i);
      if (await radio.isChecked()) {
        const id = await radio.getAttribute("id") ?? "";
        return await page.locator(`label[for="${id}"]`).textContent() ?? "";
      }
    }
    return ''
  }


}