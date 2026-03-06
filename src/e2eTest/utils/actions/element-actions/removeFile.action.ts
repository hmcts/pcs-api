import { expect, Locator, Page } from '@playwright/test';
import { actionData, IAction } from '@utils/interfaces/action.interface';

export class RemoveFileAction implements IAction {
  async execute(page: Page): Promise<void> {

    const button = page.getByRole('button', { name: 'Remove Add document' })
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
  }
}
