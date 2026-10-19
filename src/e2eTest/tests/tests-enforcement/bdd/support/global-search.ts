/**
 * XUI global search ("Search cases", `/search`) by "Other reference" (feature 16).
 *
 * pcs-api indexes every warrant and writ number as one of the case's `SearchCriteria.OtherCaseReferences`
 * (`EnforcementNumberSearchReferences`), and XUI's "Other reference" field searches exactly those. The case
 * is indexed by the decentralised Elasticsearch indexer after the event that gave the number, so a search
 * straight after approval can miss it: `expectSearchFindsCase` searches again until it shows up.
 *
 * The page is opened by URL rather than through the header's "Search" link, because which header links a
 * user gets depends on XUI's LaunchDarkly menu configuration, not on anything pcs-api controls.
 */
import { Page, expect } from '@playwright/test';
import { LONG_TIMEOUT, VERY_LONG_TIMEOUT } from 'playwright.config';
import { globalSearch, noResultFound, searchResults } from '@data/page-data-figma';
import { ensureSignedIn } from './session';

/** What a global search came back with. `unavailable` means XUI did not show the user the search page. */
export type GlobalSearchOutcome =
  | { kind: 'results'; caseReferences: string[] }
  | { kind: 'no-results' }
  | { kind: 'unavailable'; url: string };

const digitsOnly = (value: string): string => value.replace(/\D/g, '');

/**
 * Runs one global search with only "Other reference" filled in, across all services, and reads the outcome.
 * Fails on XUI's "Something went wrong" page, so a broken search is never read as "no results".
 */
export async function searchByOtherReference(page: Page, reference: string): Promise<GlobalSearchOutcome> {
  await ensureSignedIn(page);
  await page.goto(`${process.env.MANAGE_CASE_BASE_URL}/search`);
  const searchHeading = page.getByRole('heading', { name: globalSearch.mainHeader });
  const shown = await searchHeading
    .waitFor({ state: 'visible', timeout: LONG_TIMEOUT })
    .then(() => true)
    .catch(() => false);
  if (!shown) {
    return { kind: 'unavailable', url: page.url() };
  }

  // XUI keeps the previous search in session storage and pre-fills the form with it.
  for (const field of ['#caseRef', '#fullName', '#addressLine1', '#postcode', '#email']) {
    await page.locator(field).fill('');
  }
  await page.locator('#otherRef').fill(reference);
  await page.getByRole('button', { name: globalSearch.searchButton, exact: true }).click();

  const resultsHeading = page.getByRole('heading', { name: searchResults.mainHeader });
  const noResultsHeading = page.getByRole('heading', { name: noResultFound.mainHeader });
  const errorHeading = page.getByRole('heading', { name: 'Something went wrong' });
  await expect(resultsHeading.or(noResultsHeading).or(errorHeading)).toBeVisible({ timeout: VERY_LONG_TIMEOUT });

  if (await errorHeading.isVisible()) {
    throw new Error(`Global search for other reference "${reference}" failed: XUI showed "Something went wrong"`);
  }
  if (await noResultsHeading.isVisible()) {
    return { kind: 'no-results' };
  }
  const caseReferences = (await page.locator('tbody .case-reference').allInnerTexts()).map(digitsOnly);
  return { kind: 'results', caseReferences };
}

/** Searches by `reference` until the results include `caseReference`, allowing for indexing lag. */
export async function expectSearchFindsCase(page: Page, reference: string, caseReference: string): Promise<void> {
  const wanted = digitsOnly(caseReference);
  let last: GlobalSearchOutcome | undefined;
  await expect
    .poll(
      async () => {
        last = await searchByOtherReference(page, reference);
        return last.kind === 'results' && last.caseReferences.includes(wanted);
      },
      {
        message: `global search by other reference "${reference}" to find case ${caseReference}`,
        timeout: 3 * VERY_LONG_TIMEOUT,
        intervals: [2_000, 5_000, 10_000],
      }
    )
    .toBe(true);
  expect(last?.kind).toBe('results');
}
