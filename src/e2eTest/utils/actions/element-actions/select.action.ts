import { Locator, Page } from '@playwright/test';
import { IAction, actionRecord } from '../../interfaces/action.interface';
import { SHORT_TIMEOUT } from '../../../playwright.config';

export class SelectAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, option: string | number): Promise<void> {
    const label = typeof fieldName === 'string' ? fieldName : String(fieldName.dropdown);
    const index = typeof fieldName === 'string' ? undefined : Number(fieldName.index);
    const select = await this.resolveSelect(page, label, index);

    if (typeof option === 'number') {
      await select.selectOption({ index: option });
      return;
    }
    await select.selectOption(option);
  }

  /**
   * Strategies are tried in order and the first resolving to *exactly one* element wins. The
   * ordering is a safety property: on a page with two dropdowns the loose XPath fallbacks match
   * both, so `.first()` of those would pick the wrong one.
   */
  private async resolveSelect(page: Page, label: string, index?: number): Promise<Locator> {
    const siblingSelector = `:has-text("${label}") + select,
                             :has-text("${label}") ~ select,
                             select[name="${label}"]`;
    const strategies: Locator[] = [
      page.getByLabel(label, { exact: true }),
      page.locator(siblingSelector),
      page.locator(`//*[contains(normalize-space(.),"${label}")]//select`),
      page.locator(`//*[contains(normalize-space(.),"${label}")]/following::select[1]`),
    ];

    // count() does not poll.
    await strategies[0]
      .or(strategies[1])
      .first()
      .waitFor({ state: 'attached', timeout: SHORT_TIMEOUT })
      .catch(() => undefined);

    if (index !== undefined && !Number.isNaN(index)) {
      for (const strategy of strategies) {
        if ((await strategy.count()) > index) {
          return strategy.nth(index);
        }
      }
      return strategies[1].nth(index);
    }

    for (const strategy of strategies) {
      if ((await strategy.count()) === 1) {
        return strategy;
      }
    }
    for (const strategy of strategies) {
      if ((await strategy.count()) > 0) {
        return strategy.first();
      }
    }
    // Every strategy matched nothing, so selectOption would wait out the 40s action timeout and
    // report a bare timeout naming no dropdown. Say which label failed and what is on the page.
    const heading = await page.locator('h1').first().innerText().catch(() => '<no heading>');
    const selectCount = await page.locator('select').count().catch(() => -1);
    const labels = await page.locator('select').evaluateAll(
      nodes => nodes.map(node => node.getAttribute('id') ?? node.getAttribute('name') ?? '<unnamed>')
    ).catch(() => [] as string[]);
    throw new Error(`No dropdown matched "${label}" on page "${heading}" — `
      + `${selectCount} select(s) present: ${JSON.stringify(labels)}`);
  }
}
