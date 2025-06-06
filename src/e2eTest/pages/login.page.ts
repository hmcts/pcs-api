import { Locator, Page} from '@playwright/test';
import { BasePage } from './base.page';
import Config from "../config/config";

export class LoginPage extends BasePage {
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly signInButton: Locator;

  constructor(page: Page) {
    super(page);
    this.usernameInput = page.locator('#username');
    this.passwordInput = page.locator('#password');
    //this.signInButton = page.locator('button[name="save"]');
    //this.signInButton = page.locator('input[name="save"]');
    this.signInButton = page.locator('button[name="save"], input[name="save"][value="Sign in"]');
  }

  async login(username: string, password: string): Promise<void> {
    await this.goto(Config.manageCasesBaseURL);
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
    await this.signInButton.click();

    // Wait for URL to contain expected part
    //await this.waitForUrlToContain('cases');
  }
}
