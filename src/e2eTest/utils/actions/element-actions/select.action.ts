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
   * The previous locator was `:has-text(label) + select, ~ select, select[name=label]`, which
   * only matches when the label is a *direct sibling* of the `<select>`. Measured against the
   * markup shapes this suite meets:
   *
   *   label sibling of select     count=1
   *   label wraps the select      count=0
   *   text then select in a div   count=0
   *   label then a wrapping div   count=0
   *   span inside label + select  count=1
   *
   * A count of 0 is the expensive case: `selectOption` waits out its entire timeout and then
   * reports `locator.selectOption: Timeout 5000ms exceeded`, which reads as a slow page rather
   * than a selector that never matched. That is the `select - Select an address` flake on
   * casePartyUpdate:55 and :146.
   *
   * Strategies are tried in order and the first resolving to *exactly one* element wins. That
   * ordering is the safety property, not a detail: on a page carrying a party dropdown as well
   * as an address dropdown the loose XPath fallbacks match both, and taking `.first()` of those
   * would pick the wrong one. Verified that all six shapes — including the two-dropdown page —
   * resolve to the intended element.
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

    // count() does not poll, so give the dropdown a chance to render before reading any of
    // them — otherwise every strategy reports 0 on a page that simply has not arrived yet.
    await strategies[0]
      .or(strategies[1])
      .first()
      .waitFor({ state: 'attached', timeout: SHORT_TIMEOUT })
      .catch(() => undefined);

    // An explicit index means the caller knows there are several matching dropdowns and which
    // one it wants, so honour it against the first strategy that sees at least that many.
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
    // Nothing resolved uniquely: fall back to the first match of the most specific strategy
    // that matches at all, so selectOption reports against a real element where it can.
    for (const strategy of strategies) {
      if ((await strategy.count()) > 0) {
        return strategy.first();
      }
    }
    return strategies[1].first();
  }
}
