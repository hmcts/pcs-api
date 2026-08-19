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

  public static getRandomDate(type: string, format?: string): string {
    const formatDate = (date: Date): string => {
      const day = String(date.getDate()).padStart(2, '0');
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const year = date.getFullYear();

      if (format) {
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');

        return `${day}/${month}/${year}/${hours}/${minutes}/${seconds}`;
      }

      return `${day}/${month}/${year}`;
    };

    if (type === 'invalid') {
      return format
        ? '32/13/9999/25/61/61'
        : '32/13/9999';
    }

    const today = new Date();

    if (type === 'present') {
      return formatDate(today);
    }

    let minDate: Date;
    let maxDate: Date;

    if (type === 'future'|| type === 'validFuture') {
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

    return formatDate(randomDate);
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

  public static getGenApplicationType(length: number): string[] {
    const today = new Date().toLocaleDateString('en-GB', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });

    return Array.from(
      { length: length },
      (_, index) =>
        `General Application GA${index + 1} - submitted ${today}`
    );
  }

  public static renameDocument(fileName: string, fileDate?: string, app?: string): string {
    const baseName = fileName.replace(/\.pdf$/i, '');
    const gaNumber = app?.match(/\bGA\d+\b/i)?.[0] ?? '';
    const formattedDate = fileDate ? (() => {
      const [day, month, year] = fileDate.split('/');
      return `${day.padStart(2, '0')}${month.padStart(2, '0')}${year}`;
    })(): '';
    const parts = [baseName];

    if (formattedDate) {
      parts.push(formattedDate);
    }

    if (gaNumber) {
      parts.push(gaNumber);
    }

    return `${parts.join(' ')}.pdf`;
  }
}
