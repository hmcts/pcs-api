import { Page } from '@playwright/test';
export type actionData = string | number | boolean | object | string[] | object[];
export type actionRecord = Record<string, actionData>;
export type actionTuple = [string, actionData | actionRecord] | [string, actionData | actionRecord, actionData | actionRecord];

export class CaseManagementCommonUtils {

  public static async generateMoreThanMaxString(page: Page, label: string, input: string | number): Promise<string> {

    let length: number;

    if (input === 'MAXPLUS') {
      const hintText = await page
        .locator(`//span[text()="${label}"]/ancestor::div[contains(@class,'form-group')]//span[contains(@class,'form-hint')]`)
        .innerText();

      const limit = CaseManagementCommonUtils.retrieveLengthFromString(hintText);
      if (limit === 0) return '';

      length = limit + 1;

    } else if (typeof input === 'number') {
      length = input + 1;

    } else {
      return '';
    }

    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let finalString = '';
    for (let i = 0; i < length; i++) {
      finalString += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return finalString;
  }

  public static retrieveLengthFromString(input: string): number {
    const getCharCount = input.split('You can enter').map(str => str.trim()).filter(str => str.length > 0);
    const charLimitInfo = getCharCount[getCharCount.length - 1].match(/[-+]?(?:\d{1,3}(?:,\d{3})+|\d+)(?:\.\d+)?/);
    const amount = charLimitInfo ? Number(charLimitInfo[0].replace(/,/g, "")) : 0;
    return Number(amount.toFixed(2));
  }


  // public static inputDOB(inputArray: string[]): string {
  //   return inputArray.map((item) => item + " - " + CaseManagementCommonUtils.getRandomDate(18, 30)).join('\n');
  // }


  public static getRandomDate(type: string): string {

    if (type === 'invalid') {
      return '32/13/9999';
    }

    const today = new Date();

    if (type === 'present') {
      const day = String(today.getDate()).padStart(2, '0');
      const month = String(today.getMonth() + 1).padStart(2, '0');
      const year = today.getFullYear();

      return `${day}/${month}/${year}`;
    }

    let minDate: Date;
    let maxDate: Date;

    if (type === 'future') {
      minDate = today;
      maxDate = new Date(
        today.getFullYear() + 10,
        today.getMonth(),
        today.getDate()
      );
    } else {
      minDate = new Date(
        today.getFullYear() - 10,
        today.getMonth(),
        today.getDate()
      );
      maxDate = today;
    }

    const randomTime =
      minDate.getTime() +
      Math.random() * (maxDate.getTime() - minDate.getTime());

    const randomDate = new Date(randomTime);

    const day = String(randomDate.getDate()).padStart(2, '0');
    const month = String(randomDate.getMonth() + 1).padStart(2, '0');
    const year = randomDate.getFullYear();

    return `${day}/${month}/${year}`;
  }

  public static getRandomElementForAnArray<T>(arr: T[]): T | undefined {

    if (arr.length === 0) return undefined;
    const index = Math.floor(Math.random() * arr.length);
    return arr[index];

  }

  public static formatPayLoadData(input: string): string {

    let formattedOutput = "";
    const splitInput = input.toLowerCase().split("_");
    formattedOutput = splitInput
      .map((str, i) => (i === 0 ? str.charAt(0).toUpperCase() + str.slice(1) : str))
      .join(" ");
    return formattedOutput;

  }

  public static generateRandomString(length: string | number): string {
    if (typeof length !== 'number' || !Number.isInteger(length) || length <= 0) {
      return '';
    }
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    return Array.from({ length }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
  }


  public static getRandomNumberAsString(min: number, max: number): string {
    return (Math.floor(Math.random() * (max - min + 1)) + min).toString();
  }

} 
