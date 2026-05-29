import { Page } from '@playwright/test';
import { performAction } from '../../../controller';
import { actionRecord, IAction } from '@utils/interfaces';
import { globalSearch } from '@data/page-data-figma';
import { home } from '@data/page-data';
import { waitForPageRedirectionTimeout } from 'playwright.config';

export class GlobalSearchCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['accessingTheSearch', () => this.accessingTheSearch(page)],
      ['searchByCaseReference', () => this.searchByCaseReference(fieldName as string, page)],
      ['searchByName', () => this.searchByName(fieldName as string, page)],
      ['searchByFirstLineOfAddress', () => this.searchByFirstLineOfAddress(fieldName as string, page)],
      ['searchByPostcode', () => this.searchByPostcode(fieldName as string, page)],
      ['searchByEmailAddress', () => this.searchByEmailAddress(fieldName as string, page)],
      ['searchByDateOfBirth', () => this.searchByDateOfBirth(fieldName as actionRecord, page)],
      ['searchByService', () => this.searchByService(fieldName as string, page)],
      ['clickCaseNumberLink', () => this.clickCaseNumberLink(page)],
      ['searchByNameAndPostcode', () => this.searchByNameAndPostcode(fieldName as actionRecord, page)],
      ['searchByAddressAndPostcode', () => this.searchByAddressAndPostcode(fieldName as actionRecord, page)],
      ['searchByNameAndService', () => this.searchByNameAndService(fieldName as actionRecord, page)],
      ['searchByInvalidCombination', () => this.searchByInvalidCombination(fieldName as actionRecord, page)]
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async accessingTheSearch(page: Page): Promise<void> {
    await performAction('clickButton', home.globalSearchTab);
    await page.waitForTimeout(waitForPageRedirectionTimeout);
  }

  private async searchByCaseReference(caseReference: string, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.caseReferenceLabel, caseReference);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await this.submitSearch(page);
  }

  private async searchByName(name: string, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.nameOfAPartyLabel, name);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await this.submitSearch(page);
  }

  private async searchByFirstLineOfAddress(address: string, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.firstLineOfAddressLabel, address);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await this.submitSearch(page);
  }

  private async searchByPostcode(postcode: string, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.postCodeLabel, postcode);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await this.submitSearch(page);
  }

  private async searchByEmailAddress(email: string, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.emailAddressLabel, email);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await this.submitSearch(page);
  }

  private async searchByDateOfBirth(dob: actionRecord, page: Page): Promise<void> {
    const dobGroup = page.getByRole('group', { name: /date of birth/i });
    await dobGroup.getByLabel(/day/i).fill(dob.day as string);
    await dobGroup.getByLabel(/month/i).fill(dob.month as string);
    await dobGroup.getByLabel(/year/i).fill(dob.year as string);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await this.submitSearch(page);
  }

  private async searchByService(service: string, page: Page): Promise<void> {
    await performAction('select', globalSearch.servicesDropdownLabel, service);
    await this.submitSearch(page);
  }

  private async clickCaseNumberLink(page: Page): Promise<void> {
    const caseNumber = process.env.CASE_NUMBER;
    if (!caseNumber) throw new Error('CASE_NUMBER environment variable is not set');
    const link = page.getByRole('link', { name: caseNumber, exact: true }).first();
    await link.waitFor({ state: 'visible' });
    await link.click();
    await page.waitForURL(new RegExp(caseNumber));
  }

  private async searchByNameAndPostcode(fields: actionRecord, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.nameOfAPartyLabel, fields.name as string);
    await performAction('inputText', globalSearch.postCodeLabel, fields.postcode as string);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await this.submitSearch(page);
  }

  private async searchByAddressAndPostcode(fields: actionRecord, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.firstLineOfAddressLabel, fields.address as string);
    await performAction('inputText', globalSearch.postCodeLabel, fields.postcode as string);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await this.submitSearch(page);
  }

  private async searchByNameAndService(fields: actionRecord, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.nameOfAPartyLabel, fields.name as string);
    await performAction('select', globalSearch.servicesDropdownLabel, fields.service as string);
    await this.submitSearch(page);
  }

  private async searchByInvalidCombination(fields: actionRecord, page: Page): Promise<void> {
    if (fields.caseReference) {
      await performAction('inputText', globalSearch.caseReferenceLabel, fields.caseReference as string);
    }
    if (fields.name) {
      await performAction('inputText', globalSearch.nameOfAPartyLabel, fields.name as string);
    }
    if (fields.postcode) {
      await performAction('inputText', globalSearch.postCodeLabel, fields.postcode as string);
    }
    await this.submitSearch(page);
  }

  private async submitSearch(page: Page): Promise<void> {
    const searchButton = page.getByRole('button', { name: globalSearch.searchButton, exact: true }).first();
    await page.locator('.spinner-container').waitFor({ state: 'detached' }).catch(() => undefined);
    await searchButton.waitFor({ state: 'visible' });
    await searchButton.scrollIntoViewIfNeeded().catch(() => undefined);
    await searchButton.click();
    await page.waitForLoadState();
    await page.locator('.spinner-container').waitFor({ state: 'detached' }).catch(() => undefined);
  }
}