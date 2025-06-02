import { Base } from './base';

export class LoginPage extends Base {
  private usernameInput = '#username';
  private passwordInput = '#password';
  private signInButton = 'button[name=save]';

  async login(username: string, password: string) {
    await this.page.fill(this.usernameInput, username);
    await this.page.fill(this.passwordInput, password);
    await this.page.waitForTimeout(10000); // Waits for 2 seconds

    await this.page.click(this.signInButton);
  }
}
