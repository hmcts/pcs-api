import { expect, Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { performAction } from '@utils/controller';
import { SHORT_TIMEOUT, MEDIUM_TIMEOUT, LONG_TIMEOUT } from 'playwright.config';

export class RetryOnCallBackError implements IAction {
  /**
   * Budget note. `clickButton` can spend up to ~100s on a bad attempt (30s waiting for a
   * stale spinner to detach, 40s `actionTimeout` on the click itself, 30s on the trailing
   * spinner wait) while this wrapper allowed 20s in total. So on exactly the slow attempt
   * this action exists to diagnose, `toPass` aborted partway through the first click and the
   * callback-error check below — the entire point of the action — never ran.
   *
   * Widened to 40s, not to the ~110s a worst-case attempt could take, because the budget is
   * paid per call and the calls are sequential. `enforcement.warrantOfRestitution` makes 8 of
   * them in a single test (13 across the file) inside a 600s test timeout: at 40s the worst
   * case is 320s, leaving real margin for the rest of the journey, whereas 60s would reach
   * 480s and 110s would blow the timeout outright.
   *
   * 40s clears a normal click plus both 5s assertions comfortably. It does not guarantee two
   * full attempts on a pathologically slow click, which is the accepted trade: the point of
   * this change is that the callback-error assertion now runs at all.
   *
   * Deliberately not nesting a `toPass` around the click to bound it: that would re-click
   * Continue and submit the CCD event twice.
   */
  async execute(page: Page, action: string, button: string, nextPageElement: string,): Promise<void> {
    await expect(async () => {
      await performAction('clickButton', button);
      await expect(page.locator(`h3.error-summary-heading:text-is("The event could not be created"),
                                    h3.error-summary-heading:text-is("Errors"),
                                    h2#error-summary-title:text-is("There is a problem"),
                                    h3#edit-case-event_error-summary-heading
                                    `), `This checks for Unexpected callback errors or server failures. The action retries based on the timeout provided.`).toHaveCount(0, { timeout: SHORT_TIMEOUT });

      await expect(page.locator(`//h1[text()="${nextPageElement}"]`), `If the ${nextPageElement} page is not loaded on the initial attempt,then this retry logic will be activated =>`).toBeVisible({ timeout: SHORT_TIMEOUT });
    }).toPass({
      timeout: LONG_TIMEOUT + MEDIUM_TIMEOUT,
    });
  }
}