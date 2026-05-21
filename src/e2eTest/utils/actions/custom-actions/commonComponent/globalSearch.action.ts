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
      ['searchByCaseReference', () => this.searchByCaseReference(fieldName as string)],
      ['searchByName', () => this.searchByName(fieldName as string)],
      ['searchByFirstLineOfAddress', () => this.searchByFirstLineOfAddress(fieldName as string)],
      ['searchByPostcode', () => this.searchByPostcode(fieldName as string)],
      ['searchByEmailAddress', () => this.searchByEmailAddress(fieldName as string)],
      ['searchByDateOfBirth', () => this.searchByDateOfBirth(fieldName as actionRecord, page)],
      ['searchByService', () => this.searchByService(fieldName as string)],
      ['clickCaseNumberLink', () => this.clickCaseNumberLink(page)],
      ['searchByNameAndPostcode', () => this.searchByNameAndPostcode(fieldName as actionRecord)],
      ['searchByAddressAndPostcode', () => this.searchByAddressAndPostcode(fieldName as actionRecord)],
      ['searchByNameAndService', () => this.searchByNameAndService(fieldName as actionRecord)],
      ['searchByInvalidCombination', () => this.searchByInvalidCombination(fieldName as actionRecord)]
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async accessingTheSearch(page: Page): Promise<void> {
    await performAction('clickButton', home.globalSearchTab);
    await page.waitForTimeout(waitForPageRedirectionTimeout);
  }

  private async searchByCaseReference(caseReference: string): Promise<void> {
    await performAction('inputText', globalSearch.caseReferenceLabel, caseReference);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByName(name: string): Promise<void> {
    await performAction('inputText', globalSearch.nameOfAPartyLabel, name);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByFirstLineOfAddress(address: string): Promise<void> {
    await performAction('inputText', globalSearch.firstLineOfAddressLabel, address);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByPostcode(postcode: string): Promise<void> {
    await performAction('inputText', globalSearch.postCodeLabel, postcode);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByEmailAddress(email: string): Promise<void> {
    await performAction('inputText', globalSearch.emailAddressLabel, email);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByDateOfBirth(dob: actionRecord, page: Page): Promise<void> {
    const dobGroup = page.getByRole('group', { name: /date of birth/i });
    await dobGroup.getByLabel(/day/i).fill(dob.day as string);
    await dobGroup.getByLabel(/month/i).fill(dob.month as string);
    await dobGroup.getByLabel(/year/i).fill(dob.year as string);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByService(service: string): Promise<void> {
    await performAction('select', globalSearch.servicesDropdownLabel, service);
    await performAction('clickButton', globalSearch.search);
  }

  private async clickCaseNumberLink(page: Page): Promise<void> {
    const caseNumber = process.env.CASE_NUMBER;
    if (!caseNumber) throw new Error('CASE_NUMBER environment variable is not set');
    const link = page.getByRole('link', { name: caseNumber, exact: true }).first();
    await link.waitFor({ state: 'visible' });
    await link.click();
    await page.waitForURL(new RegExp(caseNumber));
  }

  private async searchByNameAndPostcode(fields: actionRecord): Promise<void> {
    await performAction('inputText', globalSearch.nameOfAPartyLabel, fields.name as string);
    await performAction('inputText', globalSearch.postCodeLabel, fields.postcode as string);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByAddressAndPostcode(fields: actionRecord): Promise<void> {
    await performAction('inputText', globalSearch.firstLineOfAddressLabel, fields.address as string);
    await performAction('inputText', globalSearch.postCodeLabel, fields.postcode as string);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByNameAndService(fields: actionRecord): Promise<void> {
    await performAction('inputText', globalSearch.nameOfAPartyLabel, fields.name as string);
    await performAction('select', globalSearch.servicesDropdownLabel, fields.service as string);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByInvalidCombination(fields: actionRecord): Promise<void> {
    if (fields.caseReference) {
      await performAction('inputText', globalSearch.caseReferenceLabel, fields.caseReference as string);
    }
    if (fields.name) {
      await performAction('inputText', globalSearch.nameOfAPartyLabel, fields.name as string);
    }
    if (fields.postcode) {
      await performAction('inputText', globalSearch.postCodeLabel, fields.postcode as string);
    }
    await performAction('clickButton', globalSearch.search);
  }
}