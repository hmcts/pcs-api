import { expect, Locator, Page } from '@playwright/test';
import { actionData, IAction } from '@utils/interfaces/action.interface';
import { waitForInteractive } from '@utils/common/locator.utils';

export class RemoveFileAction implements IAction {
  async execute(page: Page): Promise<void> {

    const button = page.getByRole('button', { name: 'Remove Add document' })
    // count() does not poll, so without this wait a not-yet-rendered row read as 0 and the
    // loop below did nothing at all — the files stayed, silently, and the failure surfaced
    // later as an unexpected document count.
    await waitForInteractive(button);
    const count = await button.count();
    for (let i = 0; i < count; i++) {
      await this.removeFile(page, button.first());

    }
  }

  private async removeFile(page: Page, button: Locator): Promise<void> {
    const removeButton = page.getByTitle('Remove', { exact: true });
    await button.click();
    await expect(removeButton).toBeVisible({ timeout: 5000 });
    await removeButton.click();
    // Wait for the removal to complete rather than leaving callers to sleep for it.
    await expect(removeButton).toBeHidden({ timeout: 10000 });
  }
}
