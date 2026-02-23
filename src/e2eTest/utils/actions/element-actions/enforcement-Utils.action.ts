import { Page } from '@playwright/test';
export type actionData = string | number | boolean | object | string[] | object[];
export type actionRecord = Record<string, actionData>;
export type actionTuple = [string, actionData | actionRecord] | [string, actionData | actionRecord, actionData | actionRecord];

export class EnforcementCommonUtils {
  
  public static async  generateMoreThanMaxString(page: Page, label: string, input: string | number): Promise<string> {

    let length: number;

    if (input === 'MAXPLUS') {
      const hintText = await page
        .locator(`//span[text()="${label}"]/ancestor::div[contains(@class,'form-group')]//span[contains(@class,'form-hint')]`)
        .innerText();

      const limit = await EnforcementCommonUtils.retrieveAmountFromString(hintText);
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

  public static async retrieveAmountFromString(input: string): Promise<number> {
    const getCharCount = input.split('You can enter').map(str => str.trim()).filter(str => str.length > 0);
    const charLimitInfo = getCharCount[getCharCount.length - 1].match(/[-+]?(?:\d{1,3}(?:,\d{3})+|\d+)(?:\.\d+)?/);
    const amount = charLimitInfo ? Number(charLimitInfo[0].replace(/,/g, "")) : 0;
    return Number(amount.toFixed(2));
  }

  public static async convertCurrencyToString(amount: number): Promise<string> {

    const cents = Math.round(amount * 100);
    const hasZeroDecimals = cents % 100 === 0;

    const amtString =
      new Intl.NumberFormat('en-GB', {
        style: 'currency',
        currency: 'GBP',
        minimumFractionDigits: hasZeroDecimals ? 0 : 2,
        maximumFractionDigits: hasZeroDecimals ? 0 : 2,
      });
    return amtString.format(cents / 100);
  }

  public static async isNumericString(s: string): Promise<boolean> {
    const trimmed = s.trim();
    if (trimmed === "") return false;

    // Remove commas for validation (e.g., "1,234.56")
    const noCommas = trimmed.replace(/,/g, "");

    // Optional '£' should not be considered numeric; strip it if present
    if (noCommas.startsWith("£")) return false;

    // Matches: -123, 123.45, +0.5
    return /^[-+]?\d+(\.\d+)?$/.test(noCommas);
  };

  public static async compareMaps<K, V>(
    map1: Map<K, V>,
    map2: Map<K, V>,
    equals: (v1: V, v2: V) => boolean = (v1, v2) => Object.is(v1, v2)
  ): Promise<Map<K, { a: V; b: V; }>> {
    const diff = new Map<K, { a: V; b: V }>();
    //if(map1.size != map2.size) throw new Error(`The data maps are of not equal size`);

    for (const [key, aVal] of map1) {
      if (!map2.has(key)) continue; // Only compare overlapping keys
      const bVal = map2.get(key)!;
      if (!equals(aVal, bVal)) {
        diff.set(key, { a: aVal, b: bVal });
      }
    }

    return diff;
  }

  public static async inputDOB(inputArray: string[]): Promise<string> {
    return inputArray.map((item) => item + " - " + EnforcementCommonUtils.getRandomDOBFromPast(18, 30)).join('\n');
  }

  public static async getRandomDOBFromPast(date1: number, date2: number): Promise<string> {
    const today = new Date();
    const maxDate = new Date(
      today.getFullYear() - date1,
      today.getMonth(),
      today.getDate()
    );

    const minDate = new Date(
      today.getFullYear() - date2,
      today.getMonth(),
      today.getDate()
    );
    const randomTime = minDate.getTime() + Math.random() * (maxDate.getTime() - minDate.getTime());
    const randomDate = new Date(randomTime);

    const day = String(randomDate.getDate()).padStart(2, '0');
    const month = String(randomDate.getMonth() + 1).padStart(2, '0');
    const year = randomDate.getFullYear();

    return `${day} ${month} ${year}`;
  }

  public static getRandomElementForAnArray<T>(arr: T[]): T | undefined {

    if (arr.length === 0) return undefined;
    const index = Math.floor(Math.random() * arr.length);
    return arr[index];

  }

} 
