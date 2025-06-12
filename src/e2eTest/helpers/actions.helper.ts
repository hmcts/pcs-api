import { Page, expect, Locator } from '@playwright/test';

export async function clickLink(page: Page, linkText: string): Promise<void> {
  const link: Locator = page.getByRole('link', { name: linkText });
  await link.click();
}

export async function selectDropdown(page: Page, label: string, visibleTextOrIndex: string | number): Promise<void> {
  const dropdown = page.getByLabel(label);

  if (typeof visibleTextOrIndex === 'number') {
    await dropdown.selectOption({ index: visibleTextOrIndex });

    const selectedOption = dropdown.locator('option:checked');
    const selectedText = await selectedOption.textContent();
    await expect(selectedOption).toHaveText(selectedText?.trim() || '');
  } else {
    await dropdown.selectOption({ label: visibleTextOrIndex });
    const selectedOption = dropdown.locator('option:checked');
    await expect(selectedOption).toHaveText(visibleTextOrIndex);
  }
}

export async function clickButton(page: Page, name: string): Promise<void> {
  await page.getByRole('button', { name }).click();
}

export async function fillInput(page: Page, label: string, value: string): Promise<void> {
  await page.getByRole('textbox', { name: label }).fill(value);
}

export async function expectAlertTextMatches(
  page: Page,
  pattern: RegExp,
  selector: string = '.alert-message'
) {
  const text = await page.locator(selector).innerText();
  expect(text.trim()).toMatch(pattern);
}

export async function clickLinkByText(page: Page, linkText: string, selector: string = 'a'): Promise<void> {
  await page.locator(selector, { hasText: linkText }).click();
}

export async function handleCookies(page: Page): Promise<void> {
  const acceptCookies = page.getByRole('button', { name: 'Accept additional cookies' });
  if (await acceptCookies.isVisible()) {
    await acceptCookies.click();
  }

  const hideCookies = page.getByRole('button', { name: 'Hide this cookie message' });
  if (await hideCookies.isVisible()) {
    await hideCookies.click();
  }
}

